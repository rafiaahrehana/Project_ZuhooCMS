package com.businessos.auth.authentication;

import com.businessos.auth.user.UserResponse;
import com.businessos.auth.password.ChangePasswordRequest;
import com.businessos.auth.password.ForgotPasswordRequest;
import com.businessos.auth.password.ResetPasswordRequest;
import com.businessos.auth.password.VerifyResetCodeRequest;
import com.businessos.modules.company.RegisterRequest;

import com.businessos.auth.authentication.google.GoogleAuthRequest;
import com.businessos.auth.authentication.google.GoogleRegisterRequest;
import com.businessos.auth.authentication.google.GoogleSignInResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    JwtResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void resendVerification(ResendVerificationRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    /** Validates a "forgot password" code without consuming it or touching the password. */
    void verifyResetCode(VerifyResetCodeRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);

    /**
     * Signs in with a verified Google account, or reports that the account needs registering
     * first. Multi-tenancy is why it can't just create a user: every user belongs to a company,
     * and a Google token says nothing about which one.
     */
    GoogleSignInResponse googleSignIn(GoogleAuthRequest request);

    /** Completes signup for a Google account by attaching it to a chosen company as a CLIENT. */
    LoginResponse googleRegister(GoogleRegisterRequest request);
}
