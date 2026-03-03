package edu.ustb.eldercarebackend.service.elderly.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.ustb.eldercarebackend.entity.ElderLocation;
import edu.ustb.eldercarebackend.mapper.ElderLocationMapper;
import edu.ustb.eldercarebackend.service.elderly.LocationService;
import edu.ustb.eldercarebackend.service.guardian.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class LocationServiceImpl implements LocationService {
    // 1. 注入MyBatis-Plus Mapper（操作elder_locations表）
    @Autowired
    private ElderLocationMapper elderLocationMapper;

    // 2. 注入角色权限校验服务（复用校验逻辑）
    @Autowired
    private UserRoleService userRoleService;
    @Override
    public void uploadElderLocation(Integer userId, BigDecimal latitude, BigDecimal longitude) {
        // 1. 校验：当前用户是否为老年人
        if (!userRoleService.isElder(userId)) {
            throw new RuntimeException("仅老年人用户可上传位置信息");
        }

        // 2. 校验：经纬度是否合法（避免无效数据）
        if (latitude == null || latitude.compareTo(new BigDecimal("-90")) < 0 ||
                latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new RuntimeException("纬度值非法，需在-90~90之间");
        }
        if (longitude == null || longitude.compareTo(new BigDecimal("-180")) < 0 ||
                longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new RuntimeException("经度值非法，需在-180~180之间");
        }

        // 3. 保留最新位置：删除该用户的所有历史位置（MyBatis-Plus Lambda条件）
        LambdaQueryWrapper<ElderLocation> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ElderLocation::getUserId, userId); // 条件：user_id = 传入的userId
        elderLocationMapper.delete(deleteWrapper); // 执行删除

        // 4. 插入新位置记录（构造实体对象，MyBatis-Plus自动映射字段）
        ElderLocation newLocation = new ElderLocation();
        newLocation.setUserId(userId);         // 关联的老年人ID
        newLocation.setLatitude(latitude);     // 纬度
        newLocation.setLongitude(longitude);   // 经度
        newLocation.setUpdateTime(LocalDateTime.now()); // 更新时间（当前时间）
        newLocation.setIsValid(1);             // 标记为有效位置（1=有效）
        elderLocationMapper.insert(newLocation); // 执行插入
    }

    @Override
    public ElderLocation getElderLatestLocation(Integer guardianUserId, Integer elderUserId) {
        // 1. 校验：监护人是否有权限查看该老年人
        if (!userRoleService.hasGuardianPermission(guardianUserId, elderUserId)) {
            throw new RuntimeException("您没有权限查看该老年人的位置");
        }

        // 2. 查询：该老年人的最新有效位置（按更新时间倒序，取第一条）
        LambdaQueryWrapper<ElderLocation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ElderLocation::getUserId, elderUserId) // 条件1：老年人ID匹配
                .eq(ElderLocation::getIsValid, 1)         // 条件2：仅查有效位置（is_valid=1）
                .orderByDesc(ElderLocation::getUpdateTime) // 排序：最新时间在前
                .last("LIMIT 1"); // 取第一条记录（适配MySQL，MyBatis-Plus支持拼SQL片段）

        // 执行查询：返回单条记录（无数据则为null）
        return elderLocationMapper.selectOne(queryWrapper);
    }
}
