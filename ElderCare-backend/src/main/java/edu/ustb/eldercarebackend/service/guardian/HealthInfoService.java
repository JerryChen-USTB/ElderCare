package edu.ustb.eldercarebackend.service.guardian;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.ustb.eldercarebackend.entity.HealthInfo;

import java.util.List;

/**
 * 健康信息服务接口
 */
public interface HealthInfoService extends IService<HealthInfo> {
    /**
     * 根据老人userId查询最新的各类健康指标（每种类型取最新一条）
     * @param userId 老人的users.id
     * @return 最新健康指标列表
     */
    List<HealthInfo> getLatestHealthInfoByUserId(Integer userId);
    /**
     * 查询指定老人的健康信息历史记录
     * @param userId 老人的users.id
     * @param healthType 健康类型（可选，为空则查询所有类型）
     * @param offset 分页偏移量
     * @param size 每页条数
     * @return 健康历史记录列表
     */
    List<HealthInfo> getHealthInfoHistoryByUserId(Integer userId, String healthType, int offset, int size);
}
