package edu.ustb.eldercarebackend.service.elderly.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.ElderDTO;
import edu.ustb.eldercarebackend.mapper.ElderMapper;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import edu.ustb.eldercarebackend.service.elderly.ElderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * 老人信息服务实现类
 */
@Service
public class ElderServiceImpl implements ElderService {
    
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Elder getElderByUserId(Integer userId) {
        try {
            return elderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Elder>()
                    .eq("user_id", userId)
            );
        } catch (Exception e) {
            System.err.println("查询老人信息失败: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String updateElderName(Integer userId, String name) {
        try {
            // 参数验证
            if (name == null || name.trim().isEmpty()) {
                return "姓名不能为空";
            }
            
            if (name.length() > 50) {
                return "姓名长度不能超过50个字符";
            }
            
            // 查找老人记录
            Elder elder = getElderByUserId(userId);
            if (elder == null) {
                return "老人信息不存在";
            }
            
            // 更新姓名
            elder.setName(name.trim());
            elder.setUpdatedAt(new Date());
            
            int result = elderMapper.updateById(elder);
            return result > 0 ? "姓名更新成功" : "姓名更新失败";
            
        } catch (Exception e) {
            System.err.println("更新老人姓名失败: " + e.getMessage());
            return "更新失败：系统错误";
        }
    }
    
    @Override
    public String updateElderAvatar(Integer userId, String avatarUrl) {
        try {
            // 参数验证
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                return "头像URL不能为空";
            }
            
            // 查找老人记录
            Elder elder = getElderByUserId(userId);
            if (elder == null) {
                return "老人信息不存在";
            }
            
            // 更新头像
            elder.setAvatarUrl(avatarUrl.trim());
            elder.setUpdatedAt(new Date());
            
            int result = elderMapper.updateById(elder);
            return result > 0 ? "头像更新成功" : "头像更新失败";
            
        } catch (Exception e) {
            System.err.println("更新老人头像失败: " + e.getMessage());
            return "更新失败：系统错误";
        }
    }
    
    @Override
    public String updateElderInfo(Elder elder) {
        try {
            if (elder == null || elder.getId() == null) {
                return "参数错误";
            }
            
            elder.setUpdatedAt(new Date());
            int result = elderMapper.updateById(elder);
            return result > 0 ? "信息更新成功" : "信息更新失败";
            
        } catch (Exception e) {
            System.err.println("更新老人信息失败: " + e.getMessage());
            return "更新失败：系统错误";
        }
    }

    @Override
    public ElderDTO getElderInfoByUserId(Integer userId) {
        // 1. 联查数据
        User user = userMapper.selectById(userId);
        Elder elder = elderMapper.selectOne(new QueryWrapper<Elder>().eq("user_id", userId));

        if (user == null || elder == null) {
            return null;
        }

        // 2. 转换为DTO（只返回老人基本信息，不包含关系信息）
        // 注意：由于监护人-老人是多对多关系，一个老人可能有多个监护人
        // 因此关系信息应该由调用方根据具体的 Relation 记录来设置，而不是在这里查询
        ElderDTO dto = new ElderDTO();
        dto.setId(elder.getId());
        dto.setUserId(userId);
        dto.setName(elder.getName());
        dto.setPhone(user.getPhone());
        dto.setGender(convertGender(elder.getGender()));
        dto.setAge(calculateAge(elder.getBirthday()));
        dto.setAvatarUrl(elder.getAvatarUrl());
        dto.setAddress(elder.getAddress());
        dto.setHealthCondition(elder.getHealthCondition());

        return dto;
    }

    // 新增：中英文关系转换方法
    private String convertRelationship(String englishRelation) {
        if (englishRelation == null) {
            return "未知";
        }
        switch (englishRelation) {
            case "spouse":
                return "配偶";
            case "child":
                return "子女";
            case "parent":
                return "父母";
            case "sibling":
                return "兄弟姐妹";
            case "friend":
                return "朋友";
            case "other":
                return "其他";
            default:
                return "未知";
        }
    }

    // 性别转换（male→男，female→女）
    private String convertGender(String gender) {
        if (gender == null) return "未知";
        return "male".equals(gender) ? "男" : "female".equals(gender) ? "女" : "未知";
    }

    // 计算年龄
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



}
