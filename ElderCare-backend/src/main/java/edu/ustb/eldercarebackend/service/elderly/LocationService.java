package edu.ustb.eldercarebackend.service.elderly;

import edu.ustb.eldercarebackend.entity.ElderLocation;

import java.math.BigDecimal;

/**
 * 位置信息服务接口
 */
public interface LocationService {
    /**
     * 老年人上传位置信息（仅允许 role=elder 的用户）
     * @param userId 老年人用户ID（关联 users 表的 id）
     * @param latitude 纬度
     * @param longitude 经度
     */
    void uploadElderLocation(Integer userId, BigDecimal latitude, BigDecimal longitude);

    /**
     * 监护人查询老年人最新位置（需校验绑定权限）
     * @param guardianUserId 监护人用户ID
     * @param elderUserId 老年人用户ID
     * @return 最新位置信息（含经纬度、更新时间）
     */
    ElderLocation getElderLatestLocation(Integer guardianUserId, Integer elderUserId);
}
