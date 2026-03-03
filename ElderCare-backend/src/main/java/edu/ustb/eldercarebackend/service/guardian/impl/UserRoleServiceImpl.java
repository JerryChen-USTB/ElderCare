package edu.ustb.eldercarebackend.service.guardian.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.ustb.eldercarebackend.entity.Relation;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import edu.ustb.eldercarebackend.service.guardian.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户角色与权限校验服务实现类
 */
@Service
public class UserRoleServiceImpl implements UserRoleService {
    // 1. 注入 users 表 Mapper
    @Autowired
    private UserMapper userMapper;

    // 2. 注入 relations 表 Mapper
    @Autowired
    private RelationMapper relationMapper;
    @Override
    public boolean isElder(Integer userId) {
        // 步骤1：根据 userId 查询 users 表中的用户信息
        User user = userMapper.selectById(userId);
        // 步骤2：判断用户是否存在 + 角色是否为 'elder'（忽略大小写）
        return user != null && "elder".equalsIgnoreCase(user.getRole());
    }

    @Override
    public boolean hasGuardianPermission(Integer guardianUserId, Integer elderUserId) {
        // 步骤1：构造查询条件（guardian_id = 监护人ID + elderly_id = 老年人ID）
        LambdaQueryWrapper<Relation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Relation::getGuardianId, guardianUserId) // 匹配监护人ID
                .eq(Relation::getElderlyId, elderUserId);    // 匹配老年人ID

        // 步骤2：查询是否存在该绑定关系（存在则返回true，不存在返回false）
        Relation relation = relationMapper.selectOne(queryWrapper);
        return relation != null;
    }
}
