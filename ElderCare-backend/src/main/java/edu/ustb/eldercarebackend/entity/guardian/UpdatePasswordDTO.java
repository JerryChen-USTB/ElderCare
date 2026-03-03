package edu.ustb.eldercarebackend.entity.guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordDTO {
    private Integer userId;     // 监护人的用户ID
    private String code;       // 验证码
    private String newPassword;// 新密码
}
