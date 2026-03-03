package edu.ustb.eldercarebackend.entity.guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePhoneDTO {
    private Integer userId;
    private String newPhone;
    private String code;
}
