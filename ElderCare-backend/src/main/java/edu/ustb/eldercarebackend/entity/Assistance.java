package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("assistance")
public class Assistance {
    @TableId(type = IdType.AUTO)
    private Integer id; // 远程志愿者协助服务ID
    
    private Integer elderId; // 关联老人ID
    private Integer volunteerId; // 关联志愿者ID
    private String appointmentContent; // 协助内容
    
    // 状态：waiting_apply, waiting_response, waiting_call, calling, in_progress, completed, cancelled
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime; // 协助开始时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime; // 协助结束时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date applyTime; // 申请提交时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt; // 创建时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt; // 更新时间
}
