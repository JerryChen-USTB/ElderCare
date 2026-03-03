package edu.ustb.eldercarebackend.service.elderly;

import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.guardian.ElderDTO;

/**
 * 老人信息服务接口
 */
public interface ElderService {
    
    /**
     * 根据用户ID获取老人信息
     * @param userId 用户ID
     * @return 老人信息
     */
    Elder getElderByUserId(Integer userId);
    
    /**
     * 更新老人姓名
     * @param userId 用户ID
     * @param name 新姓名
     * @return 更新结果
     */
    String updateElderName(Integer userId, String name);
    
    /**
     * 更新老人头像
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    String updateElderAvatar(Integer userId, String avatarUrl);
    
    /**
     * 更新老人基本信息
     * @param elder 老人信息
     * @return 更新结果
     */
    String updateElderInfo(Elder elder);
    /**
     * 通过 user_id 查询老人完整信息
     * @param userId 老人的 users.id
     * @return 老人信息DTO
     */
    ElderDTO getElderInfoByUserId(Integer userId);

}
