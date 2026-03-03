package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    private Integer id;
    private Integer userId; // 关联老人ID
    private String deviceType; // 对应enum('watch','bracelet','scale','monitor','other')
    private String deviceName;
    private String deviceModel;
    private String deviceId; // 设备唯一标识
    private Date lastActive; // 对应timestamp类型
    private String status; // 对应enum('active','inactive','lost','broken')
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
}