package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthInfo {
    private Integer id;
    private Integer userId; // 关联老人ID
    private String healthType; // 对应enum('heart_rate','blood_pressure','blood_sugar','weight','temperature','steps','other')
    private String value; // 健康数据值（用String兼容多种格式）
    private String unit; // 数据单位
    private Date recordTime; // 对应timestamp类型
    private String notes;
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
}
