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
@TableName("relations")
public class Relation {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("guardian_id")
    private Integer guardianId; // 监护者ID
    private Integer elderlyId; // 被监护老人ID
    private String relationship; // 对应enum('spouse','child','parent','sibling','friend','other')
    private String remarks; // 老年人对监护人的备注

    @TableField("is_primary")
    private Boolean isPrimary; // 对应tinyint(1)
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
}