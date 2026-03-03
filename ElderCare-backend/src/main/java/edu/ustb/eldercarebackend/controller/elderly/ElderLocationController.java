package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.entity.ElderLocation;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.service.elderly.LocationService;
import edu.ustb.eldercarebackend.service.guardian.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/elderly/location")
public class ElderLocationController {
    private static final Logger log = LoggerFactory.getLogger(ElderLocationController.class);

    @Autowired
    private LocationService locationService;

    // 替换 RelationService 为 UserRoleService（更合理）
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 老年人上传位置接口（不变）
     */
    @PostMapping("/upload")
    public ResultVO uploadElderLocation(
            @RequestParam("userId") Integer userId,
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude) {

        try {
            locationService.uploadElderLocation(userId, latitude, longitude);
            return ResultVO.success("位置上传成功");
        } catch (RuntimeException e) {
            return ResultVO.fail(e.getMessage());
        } catch (Exception e) {
            log.error("位置上传异常", e);
            return ResultVO.fail("位置上传失败，请稍后重试");
        }
    }

    /**
     * 监护人查询老年人最新位置接口（核心修改）
     */
    @GetMapping("/get/{elderUserId}")
    public ResultVO getElderLatestLocation(
            @RequestHeader("guardianUserId") Integer guardianUserId,
            @PathVariable("elderUserId") Integer elderUserId) {

        log.info("查询位置 - 监护人ID: {}, 老年人ID: {}", guardianUserId, elderUserId);

        // 1. 参数校验
        if (guardianUserId == null || elderUserId == null) {
            return ResultVO.fail("参数不完整，无法查询位置");
        }

        // 2. 权限校验：复用 UserRoleService（替代原 RelationService 逻辑）
        if (!userRoleService.hasGuardianPermission(guardianUserId, elderUserId)) {
            return ResultVO.fail("您尚未绑定该老年人，无法查看位置");
        }

        // 3. 查询位置：传递两个参数（修复报错的核心）
        try {
            ElderLocation latestLocation = locationService.getElderLatestLocation(guardianUserId, elderUserId);
            if (latestLocation != null) {
                return ResultVO.success("查询位置成功", latestLocation);
            } else {
                return ResultVO.success("暂未查询到该老年人的位置信息", null);
            }
        } catch (RuntimeException e) {
            // 捕获服务层抛出的权限异常（双重保险）
            return ResultVO.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询位置异常", e);
            return ResultVO.fail("位置查询失败，请稍后重试");
        }
    }
}