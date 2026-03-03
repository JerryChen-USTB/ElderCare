package edu.ustb.eldercarebackend.controller.guardian;

import edu.ustb.eldercarebackend.entity.HealthInfo;
import edu.ustb.eldercarebackend.service.guardian.HealthInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康信息控制器
 */
@RestController
@RequestMapping("/api/guardian/health")
public class HealthInfoController {
    @Autowired
    private HealthInfoService healthInfoService;

    /**
     * 获取指定老人的最新健康指标
     *
     * @param userId 老人的users.id
     * @return 最新健康指标列表
     */
    @GetMapping("/latestinfo/{userId}")
    public List<HealthInfo> getLatestHealthInfo(@PathVariable Integer userId) {
        return healthInfoService.getLatestHealthInfoByUserId(userId);
    }

    /**
     * 获取指定老人的健康信息历史记录
     *
     * @param userId 老人的users.id
     * @param healthType 可选参数，指定健康类型（如"heart_rate"），为空则返回所有类型
     * @param page 页码，默认第1页
     * @param size 每页条数，默认10条
     * @return 分页的健康历史记录列表
     */
    @GetMapping("/history/{userId}")
    public List<HealthInfo> getHealthHistory(
            @PathVariable Integer userId,
            @RequestParam(required = false) String healthType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // 计算分页偏移量（MyBatis默认从0开始）
        int offset = (page - 1) * size;
        return healthInfoService.getHealthInfoHistoryByUserId(userId, healthType, offset, size);
    }
}
