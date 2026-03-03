package edu.ustb.eldercarebackend.service.volunteer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.ustb.eldercarebackend.entity.Assistance;
import edu.ustb.eldercarebackend.mapper.AssistanceMapper;
import edu.ustb.eldercarebackend.service.volunteer.VolunteerAssistanceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VolunteerAssistanceServiceImpl extends ServiceImpl<AssistanceMapper, Assistance> implements VolunteerAssistanceService {

    // 新增：按 userId 查询所有远程协助记录（用 QueryWrapper）
    @Override
    public List<Assistance> getByUserId(Integer userId) {
        QueryWrapper<Assistance> queryWrapper = new QueryWrapper<>();
        // 条件：volunteer_id = userId（关联当前用户）
        queryWrapper.eq("volunteer_id", userId)
                // 按创建时间倒序
                .orderByDesc("created_at");
        // 调用 BaseMapper 自带的 selectList 执行查询
        return baseMapper.selectList(queryWrapper);
    }
}