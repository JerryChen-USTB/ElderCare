package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("guardians")
public class Guardian {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")// 关联 users 表的外键
    private Integer userId; // 关联用户ID
    private String name;
    private String gender; // 对应enum('male','female','unknown')
    private Date birthday; // 对应date类型
    private String avatarUrl;
    private String address;
    private String profession;
    private String workplace;
    private String careExperience;
    private String availability;
    private String relationshipWithElderly;
    private String verificationStatus; // 对应enum('pending','verified','rejected')
    private String verificationDocuments;
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型

    // 新增：非数据库字段，用于临时存储从users表获取的手机号
    @TableField(exist = false) // MyBatis-Plus注解：标记为非数据库字段
    private String phone;
}
