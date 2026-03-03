package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("appointments")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Integer id; // geng
    private Integer elderId; // 关联老人ID
    private Integer volunteerId; // 关联志愿者ID
    private String appointmentType; // 预约类型：doctor,nurse,rehab,therapy,other
    private String appointmentContent; // 预约内容/服务内容（包含备注信息）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime; // 服务开始时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime; // 服务结束时间
    
    private String status; // 预约状态：pending,confirmed,completed,canceled,time_out,no_show
    private String location; // 预约地点（可选）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt; // 创建时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt; // 更新时间

    @TableField(exist = false) // 标记为“非数据库字段”，仅用于逻辑传递
    private Integer userId;// 新增：用户 ID（关联 volunteer 表的 user_id）
}
