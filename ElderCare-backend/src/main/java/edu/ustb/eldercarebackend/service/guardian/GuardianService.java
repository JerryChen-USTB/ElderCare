package edu.ustb.eldercarebackend.service.guardian;

import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.Guardian;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.*;
import io.swagger.models.auth.In;

import java.util.List;
import java.util.Map;

/**
 * 监护人服务接口
 */
public interface GuardianService {
    /**
     * 根据用户ID查询完整的监护人信息
     * @param userId 关联的用户ID（users表的id）
     * @return 完整的Guardian实体对象
     */
    Guardian getGuardianByUserId(Integer userId);

    /**
     * 更新监护人信息（支持部分字段更新）
     * @param updateDTO 包含需要更新的字段的DTO
     * @return 更新后的监护人实体
     */
    Guardian updateGuardianInfo(UpdateGuardianDTO updateDTO);

    /**
     * 查询监护人关联的所有老人信息
     * @param guardianUserId 监护人的users.id
     * @return 老人信息DTO列表
     */
    List<ElderDTO> getRelatedElders(Integer guardianUserId);

    /**
     * 更新监护人头像
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    String updateGuardianAvatar(Integer userId, String avatarUrl);
    /**
     * 基于user_id发送验证码（用于修改密码）
     * @param userId 监护人用户ID
     * @return 操作结果
     */
    ResultVO sendVerificationCode(Integer userId);

    /**
     * 基于user_id验证验证码并更新密码
     * @param dto 包含user_id、验证码和新密码的DTO
     * @return 操作结果
     */
    ResultVO modifyPassword(UpdatePasswordDTO dto);
    /**
     * 根据user_id查询用户的加密密码（用于前端比对新旧密码）
     * @param userId 用户ID（users表的id）
     * @return 加密后的密码哈希（如MD5值）
     */
    String getOldPasswordHash(Integer userId);
    /**
     * 发送更换手机号的验证码
     * @param userId 监护人用户ID
     * @param newPhone 新手机号
     * @return 操作结果
     */
    ResultVO sendUpdatePhoneCode(Integer userId, String newPhone);

    /**
     * 通过验证码验证并更新手机号
     * @param dto 包含用户ID、验证码和新手机号的DTO
     * @return 操作结果
     */
    ResultVO updatePhoneByCode(UpdatePhoneDTO dto);

    /**
     * 通过密码验证并更新手机号
     * @param userId 用户ID
     * @param password 密码
     * @param newPhone 新手机号
     * @return 操作结果
     */
    ResultVO updatePhoneByPassword(Integer userId, String password, String newPhone);

}
