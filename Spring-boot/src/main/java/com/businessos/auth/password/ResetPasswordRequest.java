package com.businessos.auth.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Two mutually-exclusive ways to prove the request is legitimate, checked in
// AuthServiceImpl.resetPassword():
//  - email + code: the "forgot password" numeric-code flow.
//  - token: the long-lived JWT link ClientServiceImpl.invite() emails to set an
//    initial portal password. Left as-is; the invite email links straight into
//    this same endpoint and switching it to a code would break that flow, since
//    the client never went through a "forgot password" step to receive one.
@Data
public class ResetPasswordRequest {
    private String email;
    private String code;
    private String token;
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
