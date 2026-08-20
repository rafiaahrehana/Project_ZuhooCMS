package com.businessos.auth.authentication;


import com.businessos.auth.role.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Long userId;
    private String firstName;
    private String email;
    private Role role;
    private Long companyId;
    private String accessToken;
    private String refreshToken;
}
