package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.ChangePasswordRequest;
import com.raf.zuhoo.data.model.request.ForgotPasswordRequest;
import com.raf.zuhoo.data.model.request.GoogleAuthRequest;
import com.raf.zuhoo.data.model.request.GoogleRegisterRequest;
import com.raf.zuhoo.data.model.request.LoginRequest;
import com.raf.zuhoo.data.model.request.RefreshTokenRequest;
import com.raf.zuhoo.data.model.request.RegisterRequest;
import com.raf.zuhoo.data.model.request.ResendVerificationRequest;
import com.raf.zuhoo.data.model.request.ResetPasswordRequest;
import com.raf.zuhoo.data.model.request.VerifyEmailRequest;
import com.raf.zuhoo.data.model.response.GoogleSignInResponse;
import com.raf.zuhoo.data.model.response.LoginResponse;
import com.raf.zuhoo.data.model.response.UserResponse;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class AuthRepository {

    private final ApiService apiService;

    public AuthRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void login(String email, String password, Callback<LoginResponse> callback) {

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(callback);
    }

    public void register(RegisterRequest request, Callback<UserResponse> callback) {
        apiService.register(request).enqueue(callback);
    }

    public void logout(String refreshToken, Callback<ResponseBody> callback) {
        apiService.logout(new RefreshTokenRequest(refreshToken)).enqueue(callback);
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword,
                               Callback<ResponseBody> callback) {
        apiService.changePassword(new ChangePasswordRequest(currentPassword, newPassword, confirmPassword))
                .enqueue(callback);
    }

    public void forgotPassword(String email, Callback<ResponseBody> callback) {
        apiService.forgotPassword(new ForgotPasswordRequest(email)).enqueue(callback);
    }

    public void resetPassword(String token, String newPassword, String confirmPassword,
                              Callback<ResponseBody> callback) {
        apiService.resetPassword(new ResetPasswordRequest(token, newPassword, confirmPassword))
                .enqueue(callback);
    }

    public void verifyEmail(String token, Callback<ResponseBody> callback) {
        apiService.verifyEmail(new VerifyEmailRequest(token)).enqueue(callback);
    }

    public void googleSignIn(String idToken, Callback<GoogleSignInResponse> callback) {
        apiService.googleSignIn(new GoogleAuthRequest(idToken)).enqueue(callback);
    }

    public void googleRegister(String idToken, Long companyId, String phone, String clientCompanyName,
                               String industry, String website, Callback<LoginResponse> callback) {
        apiService.googleRegister(new GoogleRegisterRequest(
                idToken, companyId, phone, clientCompanyName, industry, website)).enqueue(callback);
    }

    public void resendVerification(String email, Callback<ResponseBody> callback) {
        apiService.resendVerification(new ResendVerificationRequest(email)).enqueue(callback);
    }
}
