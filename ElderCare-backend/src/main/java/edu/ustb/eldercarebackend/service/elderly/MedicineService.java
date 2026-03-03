package edu.ustb.eldercarebackend.service.elderly;

import edu.ustb.eldercarebackend.entity.guardian.MedicationDTO;

import java.util.List;

/**
 * 用药记录服务接口
 */
public interface MedicineService {
    /**
     * 通过用户ID查询用药记录并转换为DTO
     * @param userId 老人关联的用户ID
     * @return 用药记录DTO列表
     */
    List<MedicationDTO> findMedicationByElderUserId(Integer userId);

}
