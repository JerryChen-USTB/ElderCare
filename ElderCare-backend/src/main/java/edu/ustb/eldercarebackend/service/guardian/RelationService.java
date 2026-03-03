package edu.ustb.eldercarebackend.service.guardian;

import edu.ustb.eldercarebackend.entity.guardian.ResultVO;

/**
 * 监护人绑定用户服务接口
 */
public interface RelationService {
    /**
     * 发送验证码
     * @param targetPhone 目标手机号
     * @return 结果封装
     */
    ResultVO sendVerificationCode(String targetPhone);

    /**
     * 绑定被监护老人
     * @param guardianId 监护者ID
     * @param targetPhone 被监护人手机号
     * @param targetName 被监护人姓名
     * @param relation 关系
     * @param verifyCode 验证码
     * @return 结果封装
     */
    ResultVO bindElder(Integer guardianId, String targetPhone, String targetName, String relation, String verifyCode);
    /**
     * 解除与被监护老人的绑定关系
     * @param guardianId 监护者ID
     * @param elderlyId 被监护老人ID
     * @return 结果封装
     */
    ResultVO unbindElder(Integer guardianId, Integer elderlyId);

}
