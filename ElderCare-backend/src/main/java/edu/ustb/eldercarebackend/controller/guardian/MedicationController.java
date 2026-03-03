package edu.ustb.eldercarebackend.controller.guardian;

import edu.ustb.eldercarebackend.entity.guardian.MedicationDTO;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.service.guardian.MedicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用药记录控制器（监护人视角）
 * 处理与老人用药提醒相关的请求
 */
@RestController
@RequestMapping("/api/guardian/medication")
public class MedicationController {
    @Autowired
    private MedicationService medicationService;

    /**
     * 获取指定老人的用药提醒列表
     *
     * @param guardianUserId 监护人的users.id（用于权限校验，建议从Token中获取）
     * @param elderUserId    老人的users.id（路径参数，指定要查询的老人）
     * @return 该老人的用药DTO列表（已格式化）
     */
    @GetMapping("/list/{elderUserId}")
    public ResultVO getElderMedications(
            @RequestParam Integer guardianUserId,
            @PathVariable Integer elderUserId
    ) {
        try {
            // 调用服务层查询，复用之前设计的MedicationService
            List<MedicationDTO> medicationList = medicationService.getElderMedications(
                    guardianUserId,
                    elderUserId,
                    "guardian"  // 明确角色为监护人
            );
            return ResultVO.success(medicationList);
        } catch (RuntimeException e) {
            // 捕获服务层抛出的业务异常（如无权限、老人不存在）
            return ResultVO.fail(e.getMessage());
        } catch (Exception e) {
            return ResultVO.fail("查询用药记录失败：" + e.getMessage());
        }
    }
    /**
     * 新增用药记录
     * @param guardianUserId 监护人ID（权限校验）
     * @param elderUserId 老人ID（关联的老人）
     * @param dto 前端传递的用药记录DTO
     */
    @PostMapping("/add/{elderUserId}")
    public ResultVO addMedication(
            @RequestParam Integer guardianUserId,
            @PathVariable Integer elderUserId,
            @RequestBody MedicationDTO dto
    ) {
        try {
            medicationService.addMedication(guardianUserId, elderUserId, dto);
            return ResultVO.success("新增用药记录成功");
        } catch (RuntimeException e) {
            return ResultVO.fail(e.getMessage());
        } catch (Exception e) {
            return ResultVO.fail("新增用药记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除用药记录
     * @param guardianUserId 监护人ID（权限校验）
     * @param medicationId 要删除的用药记录ID
     */
    @DeleteMapping("/delete/{medicationId}")
    public ResultVO deleteMedication(
            @RequestParam Integer guardianUserId,
            @PathVariable Integer medicationId
    ) {
        try {
            medicationService.deleteMedication(guardianUserId, medicationId);
            return ResultVO.success("删除用药记录成功");
        } catch (RuntimeException e) {
            return ResultVO.fail(e.getMessage());
        } catch (Exception e) {
            return ResultVO.fail("删除用药记录失败：" + e.getMessage());
        }
    }
}
