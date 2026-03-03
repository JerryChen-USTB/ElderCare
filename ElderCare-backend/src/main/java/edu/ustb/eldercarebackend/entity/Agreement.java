package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agreement {
    private Integer id;
    private String type;
    private String title;
    private String content;
    private String version;
    private Date effectiveDate;
    private Boolean isCurrent;
    private Date updatedAt;
}
