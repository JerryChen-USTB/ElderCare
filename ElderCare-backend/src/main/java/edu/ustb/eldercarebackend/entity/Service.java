package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    private Integer id;
    private Integer volunteerId; // 关联志愿者ID
    private Integer elderlyId; // 关联老人ID
    private String serviceType; // 对应enum('companion','shopping','medical','housework','other')
    private Date serviceTime; // 对应timestamp类型
    private String serviceContent;
    private Integer durationMinutes; // 服务时长(分钟)
    private Integer rating; // 服务评分(1-5)
    private String feedback;
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
    private String serviceStatus; // 对应enum('pending','confirmed','completed','canceled')
}