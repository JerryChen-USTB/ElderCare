package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.entity.guardian.MedicationDTO;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.service.elderly.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/elder/medications")
public class MedicineController {
    // 注入 elderly 模块的 MedicineService
    @Autowired
    private MedicineService medicineService;

    /**
     * 根据老人关联的 user_id 查询用药记录（返回 DTO 格式）
     * @param userId 老人关联的用户 ID（必填）
     * @return ResultVO：包含用药记录 DTO 列表或提示信息
     */
    @GetMapping("/by-user-id") // 接口子路径：/api/elderly/medications/by-user-id
    public ResultVO getMedicationByElderUserId(@RequestParam Integer userId) {
        // 1. 参数校验：避免无效的 user_id（null 或 <=0 均为无效）
        if (userId == null || userId <= 0) {
            return ResultVO.fail("无效的老人用户ID，请传入正整数");
        }

        // 2. 调用服务层方法，查询用药记录 DTO 列表
        List<MedicationDTO> medicationDTOList = medicineService.findMedicationByElderUserId(userId);

        // 3. 根据查询结果返回不同提示
        if (medicationDTOList.isEmpty()) {
            return ResultVO.success("未查询到该老人的用药记录", medicationDTOList);
        } else {
            return ResultVO.success("查询到" + medicationDTOList.size() + "条老人用药记录", medicationDTOList);
        }
    }
}
