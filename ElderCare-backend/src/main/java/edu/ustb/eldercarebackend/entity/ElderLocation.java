package edu.ustb.eldercarebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("elder_locations")
public class ElderLocation {
    @TableId(type = IdType.AUTO)
    private Integer id;                 // 主键ID
    private Integer userId;             // 关联users表的id（老年人用户ID）
    private BigDecimal latitude;        // 纬度
    private BigDecimal longitude;       // 经度
    private LocalDateTime updateTime;   // 更新时间（自动生成）
    private Integer isValid;            // 是否有效（1-有效，0-无效）
}
