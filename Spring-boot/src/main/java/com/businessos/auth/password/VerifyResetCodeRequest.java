package com.businessos.auth.password;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Checks a "forgot password" code before the user commits to typing a new
// password (see AuthServiceImpl.verifyResetCode()) - the code itself isn't
// consumed here, only re-validated for real at resetPassword() time.
@Data
public class VerifyResetCodeRequest {
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Code is required")
    private String code;
}
