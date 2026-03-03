package edu.ustb.eldercarebackend.service.guardian.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.Relation;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.mapper.ElderMapper;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import edu.ustb.eldercarebackend.service.guardian.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RelationServiceImpl implements RelationService {

    private static final Logger log = LoggerFactory.getLogger(RelationServiceImpl.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private RelationMapper relationMapper;

    // 移除RedisTemplate依赖（无需注入）
    // @Autowired
    // private RedisTemplate<String, String> redisTemplate;

    private static final ConcurrentHashMap<String, String> codeCache = new ConcurrentHashMap<>();
    // 全局定时任务线程池（替代Timer，避免资源泄露）
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public ResultVO sendVerificationCode(String targetPhone) {
        // 1. 验证手机号是否存在且是老人账号（原逻辑不变）
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("phone", targetPhone));
        if (user == null) {
            return ResultVO.fail("该手机号未注册");
        }
        Elder elder = elderMapper.selectOne(new QueryWrapper<Elder>().eq("user_id", user.getId()));
        if (elder == null) {
            return ResultVO.fail("该账号不是老人账号");
        }

        // 2. 生成6位验证码
        String verifyCode = String.format("%06d", new Random().nextInt(999999));

        // 3. 存入内存缓存（5分钟后自动过期）
        codeCache.put(targetPhone, verifyCode);
        // 用ScheduledExecutorService替代Timer，避免资源泄露
        scheduler.schedule(() -> codeCache.remove(targetPhone), 5, TimeUnit.MINUTES);

        System.out.println("【模拟发送】验证码：" + verifyCode);
        // 关键修改：将验证码作为 data 返回给前端（模仿注册接口）
        return ResultVO.success("验证码已发送（开发模式）", verifyCode);
    }

    @Transactional
    @Override
    public ResultVO bindElder(Integer guardianId, String targetPhone, String targetName, String relation, String verifyCode) {
        // 新增日志：检查传入的 guardianId 是否为 null
        log.info("bindElder 方法接收的 guardianId: {}", guardianId);
        if (guardianId == null) {
            return ResultVO.fail("guardianId 为 null，无法绑定");
        }
        // 1. 验证验证码
        String storedCode = codeCache.get(targetPhone);
        log.debug("验证码检查 - 输入: {}, 存储: {}", verifyCode, storedCode);
        if (storedCode == null) {
            log.warn("验证码已过期 - targetPhone: {}", targetPhone);
            return ResultVO.fail("验证码已过期，请重新获取");
        }
        if (!verifyCode.equals(storedCode)) {
            log.warn("验证码不匹配 - 输入: {}, 期望: {}", verifyCode, storedCode);
            return ResultVO.fail("验证码错误");
        }

        // 2. 验证被监护人信息
        User targetUser = userMapper.selectOne(new QueryWrapper<User>().eq("phone", targetPhone));
        if (targetUser == null) {
            log.warn("被监护人账号不存在 - phone: {}", targetPhone);
            return ResultVO.fail("被监护人账号不存在");
        }

        Elder targetElder = elderMapper.selectOne(new QueryWrapper<Elder>().eq("user_id", targetUser.getId()));
        if (targetElder == null) {
            log.warn("账号不是老人账号 - userId: {}", targetUser.getId());
            return ResultVO.fail("被监护人不是老人账号");
        }

        if (!targetElder.getName().equals(targetName)) {
            log.warn("姓名不匹配 - 输入: {}, 实际: {}", targetName, targetElder.getName());
            return ResultVO.fail("姓名与账号信息不符");
        }

        // 3. 检查是否已绑定
        Relation existingRelation = relationMapper.selectOne(new QueryWrapper<Relation>()
                .eq("guardian_id", guardianId)
                .eq("elderly_id", targetUser.getId()));
        if (existingRelation != null) {
            log.warn("已存在绑定关系 - guardianId: {}, elderlyId: {}", guardianId, targetUser.getId());
            return ResultVO.fail("已绑定该老人，无需重复绑定");
        }

        // 4. 创建绑定关系
        try {
            String relationEnum = convertRelationToEnum(relation);
            Relation newRelation = new Relation();
            newRelation.setGuardianId(guardianId); // 确保设置 guardianId
            newRelation.setElderlyId(targetUser.getId());
            newRelation.setRelationship(relationEnum);
            newRelation.setIsPrimary(false);
            newRelation.setCreatedAt(new Date());
            newRelation.setUpdatedAt(new Date());

            // 打印日志检查参数
            log.info("准备插入的关系数据: {}", newRelation);

            int result = relationMapper.insert(newRelation);
            log.info("绑定关系创建结果 - 影响行数: {}", result);

            codeCache.remove(targetPhone);
            return ResultVO.success("绑定成功");
        } catch (Exception e) {
            log.error("绑定过程中出现异常", e);
            return ResultVO.fail("绑定失败: " + e.getMessage());
        }
    }

    @Override
    public ResultVO unbindElder(Integer guardianId, Integer elderlyId) {
        log.info("解除绑定关系 - guardianId: {}, elderlyId: {}", guardianId, elderlyId);

        // 参数校验
        if (guardianId == null || elderlyId == null) {
            log.warn("解除绑定失败，guardianId或elderlyId为null");
            return ResultVO.fail("解除绑定失败，参数不完整");
        }

        // 检查绑定关系是否存在
        QueryWrapper<Relation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("guardian_id", guardianId)
                .eq("elderly_id", elderlyId);

        Relation existingRelation = relationMapper.selectOne(queryWrapper);
        if (existingRelation == null) {
            log.warn("解除绑定失败，绑定关系不存在 - guardianId: {}, elderlyId: {}", guardianId, elderlyId);
            return ResultVO.fail("解除绑定失败，绑定关系不存在");
        }

        // 执行删除操作
        try {
            int result = relationMapper.delete(queryWrapper);
            if (result > 0) {
                log.info("解除绑定成功 - guardianId: {}, elderlyId: {}", guardianId, elderlyId);
                return ResultVO.success("解除绑定成功");
            } else {
                log.warn("解除绑定失败，数据库操作未影响任何记录 - guardianId: {}, elderlyId: {}", guardianId, elderlyId);
                return ResultVO.fail("解除绑定失败，请重试");
            }
        } catch (Exception e) {
            log.error("解除绑定过程中出现异常", e);
            return ResultVO.fail("解除绑定失败: " + e.getMessage());
        }
    }


    /**
     * 中文关系转数据库枚举值
     */
    private String convertRelationToEnum(String chineseRelation) {
        switch (chineseRelation) {
            case "配偶":
                return "spouse";
            case "子女":
                return "child";
            case "父亲":
            case "母亲":
                return "parent"; // 父亲、母亲统一映射为parent
            case "祖父/母":
                return "grandparent"; // 新增祖父/母映射
            case "其他":
                return "other";
            default:
                log.warn("未匹配的关系类型: {}", chineseRelation); // 补充日志便于调试
                return "other";
        }
    }
}
