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
@TableName("elders")
public class Elder {
    @TableId(type = IdType.AUTO)
    private Integer id;                         // 老人ID
    private Integer userId;                     // 关联用户ID
    private String name;                        // 老人姓名
    private String gender;                      // 老人性别，对应enum('male','female','unknown')
    private Date birthday;                      // 老人出生日期，对应date类型
    private String address;                     // 老人家庭住址
    private String avatarUrl;                   // 老人头像URL
    private String healthCondition;             // 老人健康状况
    private String medicalHistory;              // 老人病史
    private String dailyCareNeeds;              // 老人日常护理需求
    private String preferences;                 // 老人偏好
    private String emergencyContactName;        // 紧急联系人姓名
    private String emergencyContactPhone;       // 紧急联系人电话
    private String emergencyContactRelation;    // 紧急联系人关系
    private Date createdAt;                     // 创建时间，对应timestamp类型
    private Date updatedAt;                     // 更新时间，对应timestamp类型
}