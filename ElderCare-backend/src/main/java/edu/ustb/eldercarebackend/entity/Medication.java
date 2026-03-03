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
@TableName("medications")
public class Medication {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")
    private Integer userId; // 关联老人ID
    private String medicineName;
    private String dosage; // 剂量（用String兼容多种格式）
    private String frequency; // 服用频率
    private Date startDate; // 对应date类型
    private Date endDate; // 对应date类型
    private Date expireDate;//新增-药物过期时间
    private String notes;
    private Boolean isActive; // 对应tinyint(1)
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
}
