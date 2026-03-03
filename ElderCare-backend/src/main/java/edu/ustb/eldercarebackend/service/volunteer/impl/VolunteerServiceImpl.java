package edu.ustb.eldercarebackend.service.volunteer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Volunteer;
import edu.ustb.eldercarebackend.mapper.VolunteerMapper;
import edu.ustb.eldercarebackend.service.volunteer.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Service
public class VolunteerServiceImpl implements VolunteerService {

    @Autowired
    private VolunteerMapper volunteerMapper; // Service层注入Mapper

    // 实现：根据ID查询志愿者
    @Override
    public Volunteer getVolunteerById(Integer id) {
        return volunteerMapper.selectById(id);
    }

    // 实现：根据ID更新志愿者信息
    @Override
    public int updateVolunteerById(Volunteer volunteer) {
        return volunteerMapper.updateById(volunteer);
    }

    // 其他已有方法的实现...
    @Override
    public Volunteer getVolunteerByUserId(Integer userId) {
        QueryWrapper<Volunteer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return volunteerMapper.selectOne(queryWrapper);
    }

    @Override
    public String updateVolunteerInfo(Volunteer volunteer) {
        // 这里可以保留原有的业务逻辑（如参数校验）
        return "修改结果"; // 实际项目中根据逻辑返回
    }
    public String updateVolunteerAvatar(Integer userId, String avatarUrl) {
        try {
            // 1. 参数验证
            if (userId == null) {
                return "用户ID不能为空";
            }
            if (!StringUtils.hasText(avatarUrl)) {
                return "头像URL不能为空";
            }

            // 2. 查询志愿者信息
            Volunteer volunteer = getVolunteerByUserId(userId);  // 修改：查询志愿者信息
            if (volunteer == null) {  // 修改：判断志愿者是否存在
                return "志愿者信息不存在";  // 修改：错误提示信息
            }

            // 3. 更新头像URL并保存
            volunteer.setAvatarUrl(avatarUrl.trim());  // 修改：更新志愿者头像URL
            volunteer.setUpdatedAt(new Date()); // 假设Volunteer实体有updatedAt字段，若没有可删除此行

            int rows = volunteerMapper.updateById(volunteer);  // 修改：使用志愿者Mapper更新
            return rows > 0 ? "头像更新成功" : "头像更新失败";

        } catch (Exception e) {
            log.error("更新志愿者头像失败，userId: {}");  // 修改：日志信息
            return "更新失败：系统错误";
        }
    }
}