package com.raf.zuhoo.ui.support;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.raf.zuhoo.R;
import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.data.chat.ChatSocket;
import com.raf.zuhoo.data.model.response.SupportMessageResponse;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.data.repository.SupportTicketRepository;
import com.raf.zuhoo.ui.common.Event;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Mirrors ServiceRequestDetailViewModel — same reasons: the ticket, its thread and the chat
// subscription all have to outlive a rotation.
public class SupportTicketDetailViewModel extends AndroidViewModel {

    private final SupportTicketRepository supportTicketRepository;
    private final ChatSocket chatSocket;

    private final MutableLiveData<SupportTicketResponse> ticket = new MutableLiveData<>();
    private final MutableLiveData<List<SupportMessageResponse>> messages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> chatLive = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<ApiErrors.ApiError>> apiError = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> messageSent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> rated = new MutableLiveData<>(false);

    private final List<SupportMessageResponse> thread = new ArrayList<>();

    private long ticketId = -1;
    private ChatSocket.Subscription subscription;
    private ChatSocket.ConnectionListener connectionListener;

    public SupportTicketDetailViewModel(@NonNull Application application) {
        super(application);
        supportTicketRepository = new SupportTicketRepository(application);
        chatSocket = ZuhooApplication.graph().chatSocket();
    }

    public LiveData<SupportTicketResponse> ticket() {
        return ticket;
    }

    public LiveData<List<SupportMessageResponse>> messages() {
        return messages;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<Boolean> chatLive() {
        return chatLive;
    }

    public LiveData<Event<Integer>> message() {
        return message;
    }

    public LiveData<Event<ApiErrors.ApiError>> apiError() {
        return apiError;
    }

    public LiveData<Event<Boolean>> messageSent() {
        return messageSent;
    }

    public LiveData<Boolean> rated() {
        return rated;
    }

    public void start(long id) {

        if (ticketId == id) {
            return;
        }

        ticketId = id;

        loadDetail();
        loadMessages();
        connectChat();
    }

    private void loadDetail() {

        loading.setValue(true);

        supportTicketRepository.getTicket(ticketId, new Callback<SupportTicketResponse>() {

            @Override
            public void onResponse(Call<SupportTicketResponse> call,
                                   Response<SupportTicketResponse> response) {

                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    message.setValue(new Event<>(R.string.error_ticket_load_failed));
                    return;
                }

                ticket.setValue(response.body());
            }

            @Override
            public void onFailure(Call<SupportTicketResponse> call, Throwable t) {
                loading.setValue(false);
                message.setValue(new Event<>(R.string.error_ticket_load_failed));
            }
        });
    }

    private void loadMessages() {

        supportTicketRepository.getMessages(ticketId, new Callback<List<SupportMessageResponse>>() {

            @Override
            public void onResponse(Call<List<SupportMessageResponse>> call,
                                   Response<List<SupportMessageResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                thread.clear();
                thread.addAll(response.body());
                messages.setValue(new ArrayList<>(thread));
            }

            @Override
            public void onFailure(Call<List<SupportMessageResponse>> call, Throwable t) {
                // messages are secondary to the main detail view — fail quietly
            }
        });
    }

    private void connectChat() {

        connectionListener = chatLive::setValue;
        chatSocket.addConnectionListener(connectionListener);

        subscription = chatSocket.subscribe(
                "/user/queue/support-tickets/" + ticketId + "/messages",
                (destination, jsonBody) ->
                        append(new Gson().fromJson(jsonBody, SupportMessageResponse.class)));
    }

    // Socket pushes and REST refetches can carry the same message, so match on the server id
    // rather than blindly appending.
    private void append(SupportMessageResponse incoming) {

        for (int i = 0; i < thread.size(); i++) {
            if (incoming.getId() != null && incoming.getId().equals(thread.get(i).getId())) {
                thread.set(i, incoming);
                messages.setValue(new ArrayList<>(thread));
                return;
            }
        }

        thread.add(incoming);
        messages.setValue(new ArrayList<>(thread));
    }

    public void sendMessage(String content, String attachmentUrl, String attachmentFileName) {

        supportTicketRepository.sendMessage(ticketId, content, attachmentUrl, attachmentFileName,
                new Callback<SupportMessageResponse>() {

            @Override
            public void onResponse(Call<SupportMessageResponse> call,
                                   Response<SupportMessageResponse> response) {

                messageSent.setValue(new Event<>(response.isSuccessful()));

                if (!response.isSuccessful() || response.body() == null) {
                    apiError.setValue(new Event<>(ApiErrors.describe(response,
                            getApplication().getString(R.string.error_comment_failed))));
                    return;
                }

                // The socket never echoes your own message back, so append it locally.
                append(response.body());
            }

            @Override
            public void onFailure(Call<SupportMessageResponse> call, Throwable t) {
                messageSent.setValue(new Event<>(false));
                message.setValue(new Event<>(R.string.error_comment_failed));
            }
        });
    }

    // Both query params are required server-side — feedback has no required=false, so an empty
    // string is sent rather than omitting it.
    public void submitSatisfaction(int rating, String feedback) {

        supportTicketRepository.submitSatisfaction(ticketId, rating, feedback,
                new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    apiError.setValue(new Event<>(ApiErrors.describe(response,
                            getApplication().getString(R.string.error_satisfaction_failed))));
                    return;
                }

                message.setValue(new Event<>(R.string.satisfaction_submitted));
                rated.setValue(true);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                message.setValue(new Event<>(R.string.error_satisfaction_failed));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (subscription != null) {
            subscription.cancel();
        }
        if (connectionListener != null) {
            chatSocket.removeConnectionListener(connectionListener);
        }
    }
}
