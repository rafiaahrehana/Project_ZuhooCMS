package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.CreateSupportTicketRequest;
import com.raf.zuhoo.data.model.request.SendSupportMessageRequest;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.SupportCategoryResponse;
import com.raf.zuhoo.data.model.response.SupportMessageResponse;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class SupportTicketRepository {

    private final ApiService apiService;

    public SupportTicketRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getSupportCategories(Callback<List<SupportCategoryResponse>> callback) {
        apiService.getSupportCategories().enqueue(callback);
    }

    public void createTicket(String title, String description, Long categoryId, String priority,
                             Callback<SupportTicketResponse> callback) {
        apiService.createSupportTicket(
                new CreateSupportTicketRequest(title, description, categoryId, priority)).enqueue(callback);
    }

    public void getMyTickets(Callback<PageResponse<SupportTicketResponse>> callback) {
        apiService.getMySupportTickets().enqueue(callback);
    }

    public void getTicket(Long id, Callback<SupportTicketResponse> callback) {
        apiService.getSupportTicket(id).enqueue(callback);
    }

    public void getMessages(Long ticketId, Callback<List<SupportMessageResponse>> callback) {
        apiService.getSupportMessages(ticketId).enqueue(callback);
    }

    public void sendMessage(Long ticketId, String message, String attachmentUrl,
                            String attachmentFileName, Callback<SupportMessageResponse> callback) {
        apiService.sendSupportMessage(
                        new SendSupportMessageRequest(ticketId, message, attachmentUrl, attachmentFileName))
                .enqueue(callback);
    }

    public void submitSatisfaction(Long ticketId, int rating, String feedback,
                                   Callback<ResponseBody> callback) {
        apiService.submitSatisfaction(ticketId, rating, feedback).enqueue(callback);
    }
}
