package edu.ustb.eldercarebackend.service.guardian.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.ustb.eldercarebackend.entity.HealthInfo;
import edu.ustb.eldercarebackend.mapper.HealthInfoMapper;
import edu.ustb.eldercarebackend.service.guardian.HealthInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康信息服务实现类
 */
@Service
public class HealthInfoServiceImpl extends ServiceImpl<HealthInfoMapper, HealthInfo> implements HealthInfoService  {
    @Autowired
    private HealthInfoMapper healthInfoMapper;

    @Override
    public List<HealthInfo> getLatestHealthInfoByUserId(Integer userId) {
        // 1. 查询该老人所有健康记录，按记录时间倒序（最新的在前）
        QueryWrapper<HealthInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)  // 关联老人userId
                .orderByDesc("record_time");  // 按记录时间倒序

        List<HealthInfo> allHealthRecords = healthInfoMapper.selectList(queryWrapper);

        // 2. 按健康类型分组，只保留每种类型的最新一条记录
        Map<String, HealthInfo> latestRecords = new HashMap<>();
        for (HealthInfo record : allHealthRecords) {
            String healthType = record.getHealthType();
            // 如果map中没有该类型，则存入（因为列表已按时间倒序，第一条就是最新的）
            if (!latestRecords.containsKey(healthType)) {
                latestRecords.put(healthType, record);
            }
        }

        // 3. 转换为列表返回
        return new ArrayList<>(latestRecords.values());
    }

    @Override
    public List<HealthInfo> getHealthInfoHistoryByUserId(Integer userId, String healthType, int offset, int size) {
        // 1. 构建查询条件
        QueryWrapper<HealthInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);  // 必须匹配用户ID

        // 2. 如果指定了健康类型，则添加类型筛选
        if (healthType != null && !healthType.isEmpty()) {
            queryWrapper.eq("health_type", healthType);
        }

        // 3. 按记录时间倒序（最新的在前），并设置分页
        queryWrapper.orderByDesc("record_time")
                .last("LIMIT " + offset + ", " + size);  // 分页查询（适用于MySQL）

        // 4. 执行查询并返回结果
        return healthInfoMapper.selectList(queryWrapper);

    }

}
