package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String phone;       // 用户手机号（作为登录标识）
    private String password;    // 加密后的密码
    private String role;        // 对应enum('elder','guardian','volunteer','admin')
    
    @TableField("push_clientid")
    private String pushClientId; // 推送消息时的设备ID（对应数据库 push_clientid 字段）
    
    private Date createdAt;     // 对应timestamp类型
    private Date updatedAt;     // 对应timestamp类型
    private Date lastLoginAt;   // 对应timestamp类型
    private String status;      // 对应enum('active','inactive','blocked')
    
    /**
     * 自定义构造函数 - 用于创建新用户
     * @param phone 手机号
     * @param password 密码
     * @param role 用户角色
     */
    public User(String phone, String password, String role) {
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.status = "active"; // 默认状态为活跃
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
}