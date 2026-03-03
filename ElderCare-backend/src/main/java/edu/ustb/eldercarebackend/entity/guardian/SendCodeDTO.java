package edu.ustb.eldercarebackend.entity.guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendCodeDTO {
    private Integer userId; // 监护人的用户ID
}
