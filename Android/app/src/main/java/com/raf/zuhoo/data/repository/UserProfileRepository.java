package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.UpdateUserProfileRequest;
import com.raf.zuhoo.data.model.response.UserProfileResponse;

import retrofit2.Callback;

public class UserProfileRepository {

    private final ApiService apiService;

    public UserProfileRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getProfile(Callback<UserProfileResponse> callback) {
        apiService.getUserProfile().enqueue(callback);
    }

    public void updateProfile(String firstName, String lastName, String email, String phone,
                              String currentPassword, Callback<UserProfileResponse> callback) {
        apiService.updateUserProfile(new UpdateUserProfileRequest(
                firstName, lastName, email, phone, currentPassword)).enqueue(callback);
    }
}
