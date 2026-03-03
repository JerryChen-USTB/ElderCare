package edu.ustb.eldercarebackend.entity.guardian;

import edu.ustb.eldercarebackend.entity.Medication;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicationDTO {
    private Integer id; // 隐藏字段，用于后续操作
    private String medicineName; // 药品名称
    private String dosage; // 剂量
    private String frequency; // 服用频率
    private String startDate; // 开始日期（yyyy-MM-dd）
    private String endDate; // 结束日期（yyyy-MM-dd）
    private String notes; // 备注（可选）
    private String isActive; // 用药状态（中文）
    private String expireDate; // 新增：存储药物过期日期的字符串（yyyy-MM-dd）
    private String isExpired;// 新增：药物是否过期（中文：已过期/未过期/未设置）

    // 日期格式化工具（线程安全）
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 从实体转换为DTO（简化版）
    public static MedicationDTO fromEntity(Medication medication) {
        MedicationDTO dto = new MedicationDTO();
        dto.setId(medication.getId());
        dto.setMedicineName(medication.getMedicineName());

        // 剂量空值处理：null→"无"
        dto.setDosage(medication.getDosage() != null ? medication.getDosage() : "无");
        dto.setFrequency(medication.getFrequency());

        // 日期格式化（Date→LocalDate→String，线程安全）
        dto.setStartDate(formatDate(medication.getStartDate()));
        dto.setEndDate(formatDate(medication.getEndDate()));
        dto.setExpireDate(formatDate(medication.getExpireDate()));

        // 备注空值处理：null→"无"
        dto.setNotes(medication.getNotes() != null ? medication.getNotes() : "无");

        // 布尔值转中文：null→"未知"，true→"正在服用"，false→"已停用"
        dto.setIsActive(medication.getIsActive() != null
                ? (medication.getIsActive() ? "正在服用" : "已停用")
                : "未知");

        // 药物是否过期：中文描述
        dto.setIsExpired(judgeDrugExpired(medication.getExpireDate()));

        return dto;
    }

    // ---------------------- 新增工具方法 ----------------------
    /**
     * 药物是否过期的中文判断（核心逻辑）
     * @param expireDate 实体中的药物过期时间（Date类型）
     * @return 中文描述：已过期/未过期/未设置
     */
    private static String judgeDrugExpired(Date expireDate) {
        if (expireDate == null) {
            return "未设置"; // 未填写药物过期时间
        }
        // 转换为LocalDate（忽略时间，仅对比日期）
        LocalDate currentDate = LocalDate.now(); // 服务器当前日期
        LocalDate drugExpireDate = expireDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        // 过期判断：药物过期日期 < 当前日期 → 已过期
        return drugExpireDate.isBefore(currentDate) ? "已过期" : "未过期";
    }

    // 日期格式化工具方法（抽取复用）
    private static String formatDate(Date date) {
        if (date == null) return null;
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.format(DATE_FORMATTER);
    }
}
