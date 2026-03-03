package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfo {
    private Integer id;
    private String phone;
    private String email;
    private String website;
    private String wechat;
    private String address;
    private String workingHours;
    private Date updatedAt; // 对应timestamp类型
}