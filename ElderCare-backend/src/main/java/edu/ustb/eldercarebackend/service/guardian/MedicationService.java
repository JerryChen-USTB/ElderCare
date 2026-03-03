package edu.ustb.eldercarebackend.service.guardian;

import edu.ustb.eldercarebackend.entity.guardian.MedicationDTO;

import java.util.List;
/**
 * 用药记录服务接口（处理所有与用药相关的业务逻辑）
 */
public interface MedicationService {
    /**
     * 查询指定老人的用药记录（带权限校验：仅监护人/管理员可查）
     * @param operatorUserId 操作人user_id（如监护人ID）
     * @param elderUserId 老人user_id（目标查询对象）
     * @param operatorRole 操作人角色（如"guardian"/"admin"，用于权限判断）
     * @return 用药DTO列表
     */
    List<MedicationDTO> getElderMedications(Integer operatorUserId, Integer elderUserId, String operatorRole);
    /**
     * 新增用药记录（带权限校验）
     * @param guardianUserId 监护人ID（操作人）
     * @param elderUserId 老人ID（关联的老人）
     * @param dto 前端传递的用药记录数据
     */
    void addMedication(Integer guardianUserId, Integer elderUserId, MedicationDTO dto);

    /**
     * 删除用药记录（带权限校验）
     * @param guardianUserId 监护人ID（操作人）
     * @param medicationId 要删除的用药记录ID
     */
    void deleteMedication(Integer guardianUserId, Integer medicationId);

}
