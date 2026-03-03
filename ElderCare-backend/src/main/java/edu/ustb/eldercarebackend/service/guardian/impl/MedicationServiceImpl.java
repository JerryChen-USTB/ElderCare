package edu.ustb.eldercarebackend.service.guardian.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Medication;
import edu.ustb.eldercarebackend.entity.Relation;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.MedicationDTO;
import edu.ustb.eldercarebackend.mapper.MedicationMapper;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import edu.ustb.eldercarebackend.service.guardian.MedicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationServiceImpl implements MedicationService {
    @Autowired
    private MedicationMapper medicationMapper;
    @Autowired
    private RelationMapper relationMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<MedicationDTO> getElderMedications(Integer operatorUserId, Integer elderUserId, String operatorRole) {
        // 1. 参数校验
        if (operatorUserId == null || operatorRole == null) {
            throw new RuntimeException("操作人信息不完整");
        }
        if (elderUserId == null) {
            throw new RuntimeException("老人ID不能为空");
        }

        // 2. 权限校验（根据角色判断）
        if ("guardian".equals(operatorRole)) {
            // 监护人：必须与老人存在关联
            Relation relation = relationMapper.selectOne(
                    new QueryWrapper<Relation>()
                            .eq("guardian_id", operatorUserId)
                            .eq("elderly_id", elderUserId)
            );
            if (relation == null) {
                throw new RuntimeException("无权查看该老人的用药记录（未关联）");
            }
        } else if ("admin".equals(operatorRole)) {
            // 管理员：无需关联，直接有权限
        } else {
            // 其他角色：无权限
            throw new RuntimeException("当前角色无权查看用药记录");
        }

        // 3. 验证老人存在
        User elderUser = userMapper.selectById(elderUserId);
        if (elderUser == null || !"elder".equals(elderUser.getRole())) {
            throw new RuntimeException("老人信息不存在");
        }

        // 4. 查询用药记录并转换为DTO
        // 修复点：明确使用实体类字段名而非数据库字段名，确保映射正确
        QueryWrapper<Medication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", elderUserId)  // 使用实体类的userId字段而非数据库的user_id
                .orderByDesc("start_date");   // 使用实体类的startDate字段

        // 增加非空判断，确保查询结果处理更健壮
        List<Medication> medications = medicationMapper.selectList(queryWrapper);
        if (medications == null) {
            throw new RuntimeException("未查询到用药记录");
        }
        // 核心修改：遍历每条记录，重新判断是否过期/结束，更新isActive
        LocalDate currentDate = LocalDate.now();
        for (Medication med : medications) {
            // 转换日期（复用已有工具方法）
            LocalDate expireLocalDate = convertToLocalDate(med.getExpireDate());
            LocalDate endLocalDate = convertToLocalDate(med.getEndDate());

            // 重新判断状态
            boolean isDrugExpired = expireLocalDate != null && expireLocalDate.isBefore(currentDate);
            boolean isUsageEnded = endLocalDate != null && endLocalDate.isBefore(currentDate);
            boolean shouldBeActive = !(isDrugExpired || isUsageEnded);

            // 如果状态发生变化，更新数据库
            if (med.getIsActive() != shouldBeActive) {
                med.setIsActive(shouldBeActive);
                med.setUpdatedAt(new Date()); // 更新时间戳
                medicationMapper.updateById(med); // 保存到数据库
            }
        }

        return medications.stream()
                .map(MedicationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    // 新增用药记录实现
    @Override
    public void addMedication(Integer guardianUserId, Integer elderUserId, MedicationDTO dto) {
        // 1. 权限校验（复用查询时的逻辑）
        validateGuardianRelation(guardianUserId, elderUserId);
        validateElderExists(elderUserId);

        // 2. 参数校验
        if (dto.getMedicineName() == null || dto.getMedicineName().trim().isEmpty()) {
            throw new RuntimeException("药品名称不能为空");
        }
        // 新增：剂量非空校验
        if (dto.getDosage() == null || dto.getDosage().trim().isEmpty()) {
            throw new RuntimeException("剂量不能为空");
        }
        // 新增：服用频率非空校验
        if (dto.getFrequency() == null || dto.getFrequency().trim().isEmpty()) {
            throw new RuntimeException("服用频率不能为空");
        }
        if (dto.getStartDate() == null || dto.getStartDate().trim().isEmpty()) {
            throw new RuntimeException("开始日期不能为空");
        }
        // 2.2 新增：药物过期日期校验（格式+合理性）
        LocalDate drugExpireLocalDate = null;
        if (dto.getExpireDate() != null && !dto.getExpireDate().trim().isEmpty()) {
            try {
                // 格式校验：必须是 yyyy-MM-dd
                drugExpireLocalDate = LocalDate.parse(dto.getExpireDate().trim(), MedicationDTO.DATE_FORMATTER);
                // 合理性校验：药物过期日期不能早于当前日期（不能新增已过期的药物）
                if (drugExpireLocalDate.isBefore(LocalDate.now())) {
                    throw new RuntimeException("药物过期日期不能早于当前日期");
                }
            } catch (Exception e) {
                throw new RuntimeException("药物过期日期格式错误（正确格式：yyyy-MM-dd）");
            }
        }

        // 3. DTO转换为实体类（处理日期格式）
        Medication medication = new Medication();
        medication.setUserId(elderUserId);
        medication.setMedicineName(dto.getMedicineName().trim());
        medication.setDosage(dto.getDosage() != null ? dto.getDosage().trim() : null);
        medication.setFrequency(dto.getFrequency() != null ? dto.getFrequency().trim() : null);
        medication.setNotes(dto.getNotes() != null ? dto.getNotes().trim() : null);

// 日期转换（String→LocalDate→Date）
        Date startDate = parseDate(dto.getStartDate());
        Date endDate = dto.getEndDate() != null ? parseDate(dto.getEndDate()) : null;
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);

        // 3.2 新增：药物过期日期转换（String→Date）
        Date drugExpireDate = drugExpireLocalDate != null
                ? Date.from(drugExpireLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null;
        medication.setExpireDate(drugExpireDate);

// ========== 新增：过期状态判断（核心修改部分） ==========
        LocalDate currentServerDate = LocalDate.now();
        LocalDate drugExpireLocalDateConv = convertToLocalDate(drugExpireDate);
        boolean isDrugExpired = drugExpireLocalDateConv != null && drugExpireLocalDateConv.isBefore(currentServerDate);

        LocalDate endLocalDateConv = convertToLocalDate(endDate);
        boolean isUsageEnded = endLocalDateConv != null && endLocalDateConv.isBefore(currentServerDate);

        medication.setIsActive(!(isDrugExpired || isUsageEnded));
        // ========================================================
// 设置创建时间和更新时间（当前时间）
        Date now = new Date();
        medication.setCreatedAt(now);
        medication.setUpdatedAt(now);

        // 4. 保存到数据库
        int rows = medicationMapper.insert(medication);
        if (rows <= 0) {
            throw new RuntimeException("新增用药记录失败（数据库操作异常）");
        }
    }

    // 删除用药记录实现
    @Override
    public void deleteMedication(Integer guardianUserId, Integer medicationId) {
        // 1. 参数校验
        if (medicationId == null) {
            throw new RuntimeException("用药记录ID不能为空");
        }

        // 2. 查询记录是否存在
        Medication medication = medicationMapper.selectById(medicationId);
        if (medication == null) {
            throw new RuntimeException("该用药记录不存在");
        }

        // 3. 权限校验（确保监护人关联该老人）
        validateGuardianRelation(guardianUserId, medication.getUserId());

        // 4. 执行删除
        int rows = medicationMapper.deleteById(medicationId);
        if (rows <= 0) {
            throw new RuntimeException("删除用药记录失败（数据库操作异常）");
        }
    }

    // ---------------------- 工具方法（复用逻辑抽取） ----------------------
    /**
     * 验证监护人是否与老人存在关联关系
     */
    private void validateGuardianRelation(Integer guardianUserId, Integer elderUserId) {
        Relation relation = relationMapper.selectOne(
                new QueryWrapper<Relation>()
                        .eq("guardian_id", guardianUserId)
                        .eq("elderly_id", elderUserId)
        );
        if (relation == null) {
            throw new RuntimeException("无权操作：未关联该老人");
        }
    }

    /**
     * 验证老人是否存在
     */
    private void validateElderExists(Integer elderUserId) {
        User elderUser = userMapper.selectById(elderUserId);
        if (elderUser == null || !"elder".equals(elderUser.getRole())) {
            throw new RuntimeException("老人信息不存在");
        }
    }

    /**
     * 日期字符串转换为Date对象（yyyy-MM-dd）
     */
    private Date parseDate(String dateStr) {
        try {
            LocalDate localDate = LocalDate.parse(dateStr, MedicationDTO.DATE_FORMATTER);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            throw new RuntimeException("日期格式错误（正确格式：yyyy-MM-dd）");
        }
    }
    /**
     * Date对象转换为LocalDate（忽略时间部分，仅保留日期）
     */
    private LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        // 用服务器默认时区转换，避免时区偏差
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}