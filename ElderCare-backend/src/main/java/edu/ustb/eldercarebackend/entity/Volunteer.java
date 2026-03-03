package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("volunteers")
public class Volunteer {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId; // 关联用户ID
    private String name;
    private String gender; // 对应enum('male','female','unknown')
    private Date birthday; // 对应date类型
    private String avatarUrl;
    private String skills; // 技能特长
    private String availability;
    private String experience; // 志愿服务经验
    private String trainingCertificates; // 培训证书
    private String serviceArea; // 服务区域
    private String verificationStatus; // 对应enum('pending','verified','rejected')
    private String verificationDocuments; // 认证材料
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
}
