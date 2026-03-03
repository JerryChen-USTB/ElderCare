package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppInfo {
    private Integer id;
    private String name;
    private String version;
    private Integer buildNumber;
    private String description;
    private String downloadUrl;
    private Boolean isMandatory;
    private String releaseNotes;
    private Date updatedAt;
}
