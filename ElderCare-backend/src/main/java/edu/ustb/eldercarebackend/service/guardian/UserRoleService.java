package edu.ustb.eldercarebackend.service.guardian;
/**
 * 用户角色与权限校验服务接口
 * 核心：校验用户角色、监护人权限
 */
public interface UserRoleService {
    /**
     * 校验用户是否为老年人角色（通过users表的role字段判断：role = 'elder'）
     * @param userId 用户ID（关联users表的id）
     * @return true=是老年人，false=非老年人（或用户不存在）
     */
    boolean isElder(Integer userId);

    /**
     * 校验监护人是否有权限查看该老年人（通过relations表的绑定关系判断）
     * @param guardianUserId 监护人用户ID（关联users表的id，角色为'guardian'）
     * @param elderUserId 老年人用户ID（关联users表的id，角色为'elder'）
     * @return true=有权限（存在绑定关系），false=无权限（无绑定或查询失败）
     */
    boolean hasGuardianPermission(Integer guardianUserId, Integer elderUserId);
}
