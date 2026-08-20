package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.AnnouncementResponse;
import com.raf.zuhoo.data.model.response.HolidayResponse;

import java.util.List;

import retrofit2.Callback;

public class NoticeBoardRepository {

    private final ApiService apiService;

    public NoticeBoardRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getActiveAnnouncements(Callback<List<AnnouncementResponse>> callback) {
        apiService.getActiveAnnouncements().enqueue(callback);
    }

    public void getCurrentYearHolidays(Callback<List<HolidayResponse>> callback) {
        apiService.getCurrentYearHolidays().enqueue(callback);
    }
}
