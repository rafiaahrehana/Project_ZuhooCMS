package com.businessos.auth.authentication;

import com.businessos.auth.password.ChangePasswordRequest;
import com.businessos.auth.password.ForgotPasswordRequest;
import com.businessos.modules.company.RegisterRequest;
import com.businessos.auth.password.ResetPasswordRequest;
import com.businessos.auth.password.VerifyResetCodeRequest;
import com.businessos.auth.user.UserResponse;
import com.businessos.auth.authentication.google.GoogleAuthRequest;
import com.businessos.auth.authentication.google.GoogleRegisterRequest;
import com.businessos.auth.authentication.google.GoogleSignInResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Sign in with a Google account. Public, like /login — the Firebase ID token in the body is
    // what proves identity, so no session is needed to call it.
    @PostMapping("/google")
    public ResponseEntity<GoogleSignInResponse> googleSignIn(
            @Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.googleSignIn(request));
    }

    // Second step, only when /google reported registered=false: attach the Google account to a
    // chosen company as a CLIENT and return a normal session.
    @PostMapping("/google/register")
    public ResponseEntity<LoginResponse> googleRegister(
            @Valid @RequestBody GoogleRegisterRequest request) {
        return ResponseEntity.ok(authService.googleRegister(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok("Verification email sent");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("If an account exists with that email, a reset link has been sent");
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<String> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        authService.verifyResetCode(request);
        return ResponseEntity.ok("Code verified");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }
}
