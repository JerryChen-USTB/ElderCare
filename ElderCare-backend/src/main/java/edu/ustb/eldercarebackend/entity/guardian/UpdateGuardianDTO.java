package edu.ustb.eldercarebackend.entity.guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 监护人信息更新DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGuardianDTO {
    private Integer userId; // 关联的用户ID（必填）
    private String name; // 昵称（可选）
    private String gender; // 性别（可选，male/female/unknown）
    private Date birthday;// 出生日期（可选）
    private String address;
}
