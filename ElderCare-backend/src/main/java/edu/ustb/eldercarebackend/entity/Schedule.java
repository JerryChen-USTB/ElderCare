package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("schedules")  // 指定数据库表名为schedules
public class Schedule {
    @TableId(type = IdType.AUTO)    // 主键自增
    private Integer id;             // 日程ID
    
    @TableField("user_id")
    private Integer userId;         // 关联老人用户ID
    
    @TableField("type")
    private String type;            // 日程类型
    
    @TableField("time")
    private Date time;              // 日程时间
    
    @TableField(value = "content", jdbcType = org.apache.ibatis.type.JdbcType.LONGVARCHAR)
    private String content;         // 日程内容 - 明确指定为长字符串类型
    
    @TableField("location")
    private String location;        // 日程地点
    
    @TableField("status")
    private String status;          // 日程状态
    
    @TableField("reminder_time")
    private Date reminderTime;      // 提醒时间
    
    @TableField("repeat_type")
    private String repeatType;      // 重复类型
    
    @TableField("parent_schedule_id")
    private Integer parentScheduleId; // 父日程ID，-1表示为父日程，NULL表示一次性日程
    
    @TableField("appointment_id")
    private Integer appointmentId;  // 关联的志愿者服务预约ID，用于连接appointments表
    
    @TableField("created_at")
    private Date createdAt;         // 创建时间
    
    @TableField("updated_at")
    private Date updatedAt;         // 更新时间
}