package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Emergency {
    private Integer id;
    private Integer userId; // 关联老人ID
    private Integer guardianId; // 关联监护者ID
    private String situationType; // 对应enum('fall','heart_attack','stroke','accident','other')
    private String situationDescription;
    private String location;
    private Boolean isResolved; // 对应tinyint(1)
    private Date createdAt; // 对应timestamp类型
    private Date resolvedAt; // 对应timestamp类型
}
