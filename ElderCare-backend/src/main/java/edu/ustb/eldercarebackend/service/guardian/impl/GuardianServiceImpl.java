package edu.ustb.eldercarebackend.service.guardian.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.Guardian;
import edu.ustb.eldercarebackend.entity.Relation;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.*;
import edu.ustb.eldercarebackend.mapper.ElderMapper;
import edu.ustb.eldercarebackend.mapper.GuardianMapper;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import edu.ustb.eldercarebackend.service.elderly.ElderService;
import edu.ustb.eldercarebackend.service.guardian.GuardianService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 监护者服务实现类
 */
@Slf4j
@Service
public class GuardianServiceImpl implements GuardianService {
    @Autowired
    private GuardianMapper guardianMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private ElderService elderService;

    @Override
    public Guardian getGuardianByUserId(Integer userId) {
        QueryWrapper<Guardian> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        Guardian guardian = guardianMapper.selectOne(queryWrapper);
        // 新增：通过user_id关联查询users表，获取手机号并设置到Guardian实体
        if (guardian != null && guardian.getUserId() != null) {
            User user = userMapper.selectById(guardian.getUserId());
            if (user != null) {
                guardian.setPhone(user.getPhone()); // 赋值到非数据库字段phone
            }
        }
        return guardian;
    }

    @Override
    public Guardian updateGuardianInfo(UpdateGuardianDTO updateDTO) {
        // 1. 校验参数
        if (updateDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 2. 查询数据库中已存在的监护人信息
        Guardian guardian = getGuardianByUserId(updateDTO.getUserId());
        if (guardian == null) {
            throw new RuntimeException("未找到对应的监护人信息");
        }

        // 3. 部分字段更新（只更新不为空的字段）
        if (StringUtils.hasText(updateDTO.getName())) {
            guardian.setName(updateDTO.getName());
        }
        if (StringUtils.hasText(updateDTO.getGender())) {
            guardian.setGender(updateDTO.getGender());
        }
        if (updateDTO.getBirthday() != null) {
            guardian.setBirthday(updateDTO.getBirthday());
        }
        // 添加地址更新逻辑
        if (StringUtils.hasText(updateDTO.getAddress())) {
            guardian.setAddress(updateDTO.getAddress());
        }

        // 4. 执行更新操作
        int rows = guardianMapper.updateById(guardian);
        if (rows <= 0) {
            log.error("更新监护人信息失败，userId: {}", updateDTO.getUserId());
            throw new RuntimeException("更新信息失败，请重试");
        }

        log.info("更新监护人信息成功，userId: {}", updateDTO.getUserId());
        // 返回更新后的数据（重新查询，确保phone字段为最新）
        return getGuardianByUserId(updateDTO.getUserId());
    }

    @Override
    public List<ElderDTO> getRelatedElders(Integer guardianUserId) {

        log.debug("开始查询监护者ID: {} 关联的老人信息", guardianUserId); // 添加日志

        List<ElderDTO> relatedElders = new ArrayList<>();

        // 1. 查询关联关系
        List<Relation> relations = relationMapper.selectList(
                new QueryWrapper<Relation>().eq("guardian_id", guardianUserId)
        );

        log.debug("找到 {} 条关联关系", relations.size()); // 添加日志

        // 2. 批量查询老人信息并补充关系字段
        for (Relation relation : relations) {
            // 日志1：打印从数据库查询到的Relation的createdAt
            log.info("Relation[id:{}]的createdAt值：{}", relation.getId(), relation.getCreatedAt());
            ElderDTO elderDTO = elderService.getElderInfoByUserId(relation.getElderlyId());
            if (elderDTO != null) {
                // 补充关系信息
                elderDTO.setRelationship(convertRelationship(relation.getRelationship()));
                elderDTO.setIsPrimary(relation.getIsPrimary() == true);
                elderDTO.setCreatedAt(relation.getCreatedAt());
                // 日志2：打印赋值后ElderDTO的createdAt
                log.info("ElderDTO[id:{}]的createdAt值：{}", elderDTO.getId(), elderDTO.getCreatedAt());
                relatedElders.add(elderDTO);
            }
        }

        log.debug("最终返回 {} 位老人信息", relatedElders.size()); // 添加日志

        return relatedElders;
    }

    @Override
    public String updateGuardianAvatar(Integer userId, String avatarUrl) {
        try {
            // 1. 参数验证
            if (userId == null) {
                return "用户ID不能为空";
            }
            if (!StringUtils.hasText(avatarUrl)) {
                return "头像URL不能为空";
            }

            // 2. 查询监护人信息
            Guardian guardian = getGuardianByUserId(userId);
            if (guardian == null) {
                return "监护人信息不存在";
            }

            // 3. 更新头像URL并保存
            guardian.setAvatarUrl(avatarUrl.trim());
            guardian.setUpdatedAt(new Date()); // 假设Guardian实体有updatedAt字段，若没有可删除此行

            int rows = guardianMapper.updateById(guardian);
            return rows > 0 ? "头像更新成功" : "头像更新失败";

        } catch (Exception e) {
            log.error("更新监护人头像失败，userId: {}", userId, e);
            return "更新失败：系统错误";
        }
    }


    //修改密码验证码部分
    // 在GuardianServiceImpl中添加
    // 存储验证码（key: user_id, value: 验证码）
    private Map<Integer, String> verificationCodes = new ConcurrentHashMap<>();
    // 存储验证码过期时间（key: user_id, value: 过期时间戳）
    private Map<Integer, Long> codeExpireTimes = new ConcurrentHashMap<>();
    // 验证码有效期5分钟（毫秒）
    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000;

    @Override
    public ResultVO sendVerificationCode(Integer userId) {
        // 1. 验证用户ID
        if (userId == null) {
            return ResultVO.fail("用户ID不能为空");
        }

        // 2. 检查监护人是否存在
        Guardian guardian = getGuardianByUserId(userId);
        if (guardian == null) {
            return ResultVO.fail("监护人不存在");
        }

        // 3. 检查用户信息是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResultVO.fail("用户不存在");
        }

        // 4. 验证用户是否绑定手机号（仍从users表获取）
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            return ResultVO.fail("用户未绑定手机号，无法发送验证码");
        }

        // 5. 生成并存储验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        verificationCodes.put(userId, code);
        codeExpireTimes.put(userId, System.currentTimeMillis() + CODE_EXPIRE_MILLIS);

        // 6. 模拟发送验证码（手机号来自users表）
        log.info("向用户[userId: {}，手机号: {}]发送验证码: {}",
                userId, user.getPhone(), code);

        return ResultVO.success("验证码发送成功，有效期5分钟");
    }

    @Override
    public ResultVO modifyPassword(UpdatePasswordDTO dto) {
        // 1. 验证参数
        if (dto.getUserId() == null) {
            return ResultVO.fail("用户ID不能为空");
        }
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            return ResultVO.fail("验证码不能为空");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().trim().isEmpty()) {
            return ResultVO.fail("新密码不能为空");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            return ResultVO.fail("用户不存在");
        }

        // 【新增】3. 检查新密码加密后是否与旧密码相同
        String oldPasswordHash = user.getPassword(); // 旧密码哈希（已存储）
        String newPasswordHash = DigestUtils.md5DigestAsHex(dto.getNewPassword().trim().getBytes()); // 新密码加密
        if (newPasswordHash.equals(oldPasswordHash)) {
            return ResultVO.fail("新密码不能与旧密码相同");
        }

        // 4. 验证验证码是否存在
        if (!verificationCodes.containsKey(dto.getUserId())) {
            return ResultVO.fail("验证码不存在，请重新获取");
        }

        // 5. 验证验证码是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime > codeExpireTimes.get(dto.getUserId())) {
            verificationCodes.remove(dto.getUserId());
            codeExpireTimes.remove(dto.getUserId());
            return ResultVO.fail("验证码已过期，请重新获取");
        }

        // 6. 验证验证码是否正确
        if (!dto.getCode().equals(verificationCodes.get(dto.getUserId()))) {
            return ResultVO.fail("验证码不正确");
        }

        // 7. 关键修改：使用与登录一致的DigestUtils加密新密码（替换MD5Util）
        String encryptedPassword = DigestUtils.md5DigestAsHex(dto.getNewPassword().trim().getBytes());
        user.setPassword(encryptedPassword);
        user.setUpdatedAt(new Date());

        int rows = userMapper.updateById(user);
        if (rows > 0) {
            // 8. 清除验证码，防止重复使用
            verificationCodes.remove(dto.getUserId());
            codeExpireTimes.remove(dto.getUserId());
            log.info("用户[userId: {}]密码修改成功", dto.getUserId());
            return ResultVO.success("密码修改成功");
        } else {
            log.error("用户[userId: {}]密码修改失败", dto.getUserId());
            return ResultVO.fail("密码修改失败，请重试");
        }
    }

    @Override
    public String getOldPasswordHash(Integer userId) {
        // 1. 验证用户ID
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 2. 查询users表中的密码（加密后的值）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 返回加密后的密码（不返回明文，仅返回哈希）
        return user.getPassword(); // 假设users表的password字段存储的是MD5加密后的哈希
    }

    // 存储更换手机号的验证码（key: user_id，value: 验证码）
    private Map<Integer, String> phoneUpdateCodes = new HashMap<>();
    // 存储更换手机号验证码的过期时间
    private Map<Integer, Long> phoneCodeExpireTimes = new HashMap<>();

    @Override
    public ResultVO sendUpdatePhoneCode(Integer userId, String newPhone) {
        // 1. 验证参数
        if (userId == null) {
            return ResultVO.fail("用户ID不能为空");
        }
        if (!StringUtils.hasText(newPhone) || !newPhone.matches("^1[3-9]\\d{9}$")) {
            return ResultVO.fail("请输入正确的手机号");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResultVO.fail("用户不存在");
        }

        // 3. 检查新手机号是否已被使用
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("phone", newPhone));
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            return ResultVO.fail("该手机号已被其他用户使用");
        }

        // 4. 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 5. 存储验证码及过期时间
        phoneUpdateCodes.put(userId, code);
        phoneCodeExpireTimes.put(userId, System.currentTimeMillis() + CODE_EXPIRE_MILLIS);

        // 6. 模拟发送验证码
        log.info("向新手机号[{}]发送更换手机号验证码: {}", newPhone, code);

        return ResultVO.success("验证码发送成功，有效期5分钟");
    }

    @Override
    public ResultVO updatePhoneByCode(UpdatePhoneDTO dto) {
        // 1. 验证参数
        if (dto.getUserId() == null) {
            return ResultVO.fail("用户ID不能为空");
        }
        if (!StringUtils.hasText(dto.getNewPhone()) || !dto.getNewPhone().matches("^1[3-9]\\d{9}$")) {
            return ResultVO.fail("请输入正确的手机号");
        }
        if (!StringUtils.hasText(dto.getCode())) {
            return ResultVO.fail("验证码不能为空");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            return ResultVO.fail("用户不存在");
        }

        // 3. 检查验证码是否存在
        if (!phoneUpdateCodes.containsKey(dto.getUserId())) {
            return ResultVO.fail("验证码不存在，请重新获取");
        }

        // 4. 检查验证码是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime > phoneCodeExpireTimes.get(dto.getUserId())) {
            phoneUpdateCodes.remove(dto.getUserId());
            phoneCodeExpireTimes.remove(dto.getUserId());
            return ResultVO.fail("验证码已过期，请重新获取");
        }

        // 5. 检查验证码是否正确
        if (!dto.getCode().equals(phoneUpdateCodes.get(dto.getUserId()))) {
            return ResultVO.fail("验证码不正确");
        }

        // 6. 再次检查新手机号是否已被使用
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("phone", dto.getNewPhone()));
        if (existingUser != null && !existingUser.getId().equals(dto.getUserId())) {
            return ResultVO.fail("该手机号已被其他用户使用");
        }

        // 7. 更新users表的手机号
        user.setPhone(dto.getNewPhone());
        user.setUpdatedAt(new Date());
        int userRows = userMapper.updateById(user);

        if (userRows > 0) {
            // 9. 清除验证码
            phoneUpdateCodes.remove(dto.getUserId());
            phoneCodeExpireTimes.remove(dto.getUserId());

            log.info("用户[userId: {}]手机号更新成功，新手机号: {}", dto.getUserId(), dto.getNewPhone());
            return ResultVO.success("手机号更新成功");
        } else {
            log.error("用户[userId: {}]手机号更新失败", dto.getUserId());
            return ResultVO.fail("手机号更新失败，请重试");
        }
    }

    @Override
    public ResultVO updatePhoneByPassword(Integer userId, String password, String newPhone) {
        // 1. 验证参数
        if (userId == null) {
            return ResultVO.fail("用户ID不能为空");
        }
        if (!StringUtils.hasText(password)) {
            return ResultVO.fail("密码不能为空");
        }
        if (!StringUtils.hasText(newPhone) || !newPhone.matches("^1[3-9]\\d{9}$")) {
            return ResultVO.fail("请输入正确的手机号");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResultVO.fail("用户不存在");
        }

        // 3. 验证密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.trim().getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            return ResultVO.fail("密码不正确");
        }

        // 4. 检查新手机号是否已被使用
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("phone", newPhone));
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            return ResultVO.fail("该手机号已被其他用户使用");
        }

        // 5. 更新users表的手机号
        user.setPhone(newPhone);
        user.setUpdatedAt(new Date());
        int userRows = userMapper.updateById(user);

        if (userRows > 0) {
            log.info("用户[userId: {}]通过密码验证更新手机号成功，新手机号: {}", userId, newPhone);
            return ResultVO.success("手机号更新成功");
        } else {
            log.error("用户[userId: {}]手机号更新失败", userId);
            return ResultVO.fail("手机号更新失败，请重试");
        }
    }


    /**
     * 计算年龄
     */
    private Integer calculateAge(Date birthday) {
        if (birthday == null) return null;
        Calendar now = Calendar.getInstance();
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthday);
        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.MONTH) < birth.get(Calendar.MONTH)
                || (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH)
                && now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age < 0 ? 0 : age;
    }

    /**
     * 转换性别为中文（male→男，female→女）
     */
    private String convertGender(String gender) {
        if (gender == null) return "未知";
        switch (gender) {
            case "male": return "男";
            case "female": return "女";
            default: return "未知";
        }
    }

    /**
     * 转换监护关系为中文（spouse→配偶等）
     */
    private String convertRelationship(String relationship) {
        if (relationship == null) return "未知";
        switch (relationship) {
            case "spouse": return "配偶";
            case "child": return "子女";
            case "parent": return "父母";
            case "sibling": return "兄弟姐妹";
            case "friend": return "朋友";
            default: return "其他";
        }
    }
}
