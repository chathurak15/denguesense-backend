package com.zeylex.denguesense.dto.responseDTO;

import com.zeylex.denguesense.model.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String fname;
    private String lname;
    private String email;
    private String phoneNumber;
    private String status;
    private RoleType role;
    private String district;
    private String image;
    private LocalDateTime createdAt;
}
