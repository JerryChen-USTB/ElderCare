package edu.ustb.eldercarebackend.entity.guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 老人信息数据传输对象，统一接口返回格式
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElderDTO {
    private Integer id;                 // 老人表主键
    private Integer userId;             // 关联用户ID
    private String name;                // 姓名
    private String phone;               // 手机号（来自users表）
    private String gender;              // 性别（中文显示）
    private Integer age;                // 年龄（计算得出）
    private String avatarUrl;           // 头像URL
    private String address;             // 居住地址
    private String healthCondition;     // 健康状况
    private String relationship;        // 与监护人关系（来自relations表）
    private Boolean isPrimary;          // 是否为主监护关系
    private Date createdAt;
}
