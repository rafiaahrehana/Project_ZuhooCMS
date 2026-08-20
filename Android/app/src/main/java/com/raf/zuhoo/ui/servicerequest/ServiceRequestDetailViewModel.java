package com.raf.zuhoo.ui.servicerequest;

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
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.data.model.request.AddCommentRequest;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.RequestComment;
import com.raf.zuhoo.data.model.response.RequestStatusHistory;
import com.raf.zuhoo.data.model.response.ServiceRequestDetail;
import com.raf.zuhoo.data.model.response.ServiceReviewResponse;
import com.raf.zuhoo.data.repository.EmployeeRepository;
import com.raf.zuhoo.data.repository.ReviewRepository;
import com.raf.zuhoo.data.repository.ServiceRequestRepository;
import com.raf.zuhoo.ui.common.Event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Owns everything the detail screen needs, including its chat subscription — so rotating the
// device no longer drops the thread, re-opens a socket, or writes into a dead view binding.
public class ServiceRequestDetailViewModel extends AndroidViewModel {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatSocket chatSocket;
    private final boolean staff;

    private final MutableLiveData<ServiceRequestDetail> detail = new MutableLiveData<>();
    private final MutableLiveData<List<RequestComment>> comments = new MutableLiveData<>();
    private final MutableLiveData<List<RequestStatusHistory>> history = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> chatLive = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<ApiErrors.ApiError>> apiError = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> commentSent = new MutableLiveData<>();

    private final List<RequestComment> thread = new ArrayList<>();

    private long requestId = -1;
    private ChatSocket.Subscription subscription;
    private ChatSocket.ConnectionListener connectionListener;

    public ServiceRequestDetailViewModel(@NonNull Application application) {
        super(application);
        serviceRequestRepository = new ServiceRequestRepository(application);
        reviewRepository = new ReviewRepository(application);
        employeeRepository = new EmployeeRepository(application);
        chatSocket = ZuhooApplication.graph().chatSocket();

        String role = ZuhooApplication.graph().tokenManager().getRole();
        staff = Role.COMPANY_OWNER.equals(role) || Role.EMPLOYEE.equals(role);
    }

    public boolean isStaff() {
        return staff;
    }

    public LiveData<ServiceRequestDetail> detail() {
        return detail;
    }

    public LiveData<List<RequestComment>> comments() {
        return comments;
    }

    public LiveData<List<RequestStatusHistory>> history() {
        return history;
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

    public LiveData<Event<Boolean>> commentSent() {
        return commentSent;
    }

    // Re-entrant: called from onCreate on every configuration change, but only the first call for
    // a given id does any work.
    public void start(long id) {

        if (requestId == id) {
            return;
        }

        requestId = id;

        loadDetail();
        loadHistory();
        loadComments();
        connectChat();
    }

    // ── loads ────────────────────────────────────────────────────

    public void loadDetail() {

        loading.setValue(true);

        serviceRequestRepository.getServiceRequest(requestId, new Callback<ServiceRequestDetail>() {

            @Override
            public void onResponse(Call<ServiceRequestDetail> call,
                                   Response<ServiceRequestDetail> response) {

                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    message.setValue(new Event<>(R.string.error_request_load_failed));
                    return;
                }

                detail.setValue(response.body());
            }

            @Override
            public void onFailure(Call<ServiceRequestDetail> call, Throwable t) {
                loading.setValue(false);
                message.setValue(new Event<>(R.string.error_request_load_failed));
            }
        });
    }

    private void loadHistory() {

        serviceRequestRepository.getHistory(requestId, new Callback<List<RequestStatusHistory>>() {

            @Override
            public void onResponse(Call<List<RequestStatusHistory>> call,
                                   Response<List<RequestStatusHistory>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    history.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<RequestStatusHistory>> call, Throwable t) {
                // Supplementary to the main detail view — stay quiet rather than throwing an
                // error over the top of content that loaded fine.
            }
        });
    }

    private void loadComments() {

        serviceRequestRepository.getComments(requestId, new Callback<PageResponse<RequestComment>>() {

            @Override
            public void onResponse(Call<PageResponse<RequestComment>> call,
                                   Response<PageResponse<RequestComment>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                // The endpoint returns newest-first (findBy...OrderByCreatedAtDesc). Flip it so
                // the thread reads oldest-to-newest like a chat, which is what appending a
                // socket push at the bottom assumes.
                List<RequestComment> oldestFirst = new ArrayList<>(response.body().getContent());
                Collections.reverse(oldestFirst);

                thread.clear();
                thread.addAll(oldestFirst);
                comments.setValue(new ArrayList<>(thread));
            }

            @Override
            public void onFailure(Call<PageResponse<RequestComment>> call, Throwable t) {
                // as above — comments are secondary to the detail
            }
        });
    }

    // ── chat ─────────────────────────────────────────────────────

    private void connectChat() {

        connectionListener = chatLive::setValue;
        chatSocket.addConnectionListener(connectionListener);

        subscription = chatSocket.subscribe(
                "/user/queue/service-requests/" + requestId + "/messages",
                (destination, jsonBody) ->
                        appendComment(new Gson().fromJson(jsonBody, RequestComment.class)));
    }

    // Socket pushes and REST refetches can carry the same message, so match on the server id
    // rather than blindly appending.
    private void appendComment(RequestComment comment) {

        for (int i = 0; i < thread.size(); i++) {
            if (comment.getId() != null && comment.getId().equals(thread.get(i).getId())) {
                thread.set(i, comment);
                comments.setValue(new ArrayList<>(thread));
                return;
            }
        }

        thread.add(comment);
        comments.setValue(new ArrayList<>(thread));
    }

    public void sendComment(String content, String attachmentUrl) {

        // Staff comments MUST carry an explicit visibility — the backend defaults a staff
        // comment to INTERNAL, which the client can never see.
        AddCommentRequest request = staff
                ? AddCommentRequest.fromStaff(content, attachmentUrl)
                : AddCommentRequest.fromClient(content, attachmentUrl);

        serviceRequestRepository.addComment(requestId, request, new Callback<RequestComment>() {

            @Override
            public void onResponse(Call<RequestComment> call, Response<RequestComment> response) {

                commentSent.setValue(new Event<>(response.isSuccessful()));

                if (!response.isSuccessful() || response.body() == null) {
                    apiError.setValue(new Event<>(ApiErrors.describe(response,
                            getApplication().getString(R.string.error_comment_failed))));
                    return;
                }

                // The socket never echoes your own message back, so append it locally.
                appendComment(response.body());
            }

            @Override
            public void onFailure(Call<RequestComment> call, Throwable t) {
                commentSent.setValue(new Event<>(false));
                message.setValue(new Event<>(R.string.error_comment_failed));
            }
        });
    }

    // ── client actions ───────────────────────────────────────────

    public void acceptQuotation() {
        detailAction(callback -> serviceRequestRepository.acceptQuotation(requestId, callback),
                R.string.quotation_accepted, R.string.error_quotation_action_failed);
    }

    public void rejectQuotation(String reason) {
        detailAction(callback -> serviceRequestRepository.rejectQuotation(requestId, reason, callback),
                R.string.quotation_rejected, R.string.error_quotation_action_failed);
    }

    public void cancelRequest() {

        serviceRequestRepository.cancelServiceRequest(requestId, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    apiError.setValue(new Event<>(ApiErrors.describe(response,
                            getApplication().getString(R.string.error_cancel_failed))));
                    return;
                }

                message.setValue(new Event<>(R.string.request_cancelled));
                loadDetail();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                message.setValue(new Event<>(R.string.error_cancel_failed));
            }
        });
    }

    public void submitReview(int rating, String comment) {

        reviewRepository.submitReview(requestId, rating, comment, new Callback<ServiceReviewResponse>() {

            @Override
            public void onResponse(Call<ServiceReviewResponse> call,
                                   Response<ServiceReviewResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    apiError.setValue(new Event<>(ApiErrors.describe(response,
                            getApplication().getString(R.string.error_review_failed))));
                    return;
                }

                message.setValue(new Event<>(R.string.review_submitted));
            }

            @Override
            public void onFailure(Call<ServiceReviewResponse> call, Throwable t) {
                message.setValue(new Event<>(R.string.error_review_failed));
            }
        });
    }

    // ── staff actions ────────────────────────────────────────────

    public void loadEmployees(Callback<PageResponse<EmployeeResponse>> callback) {
        employeeRepository.getEmployees(callback);
    }

    public void assign(Long employeeId) {
        detailAction(callback -> serviceRequestRepository.assignServiceRequest(requestId, employeeId, callback),
                R.string.request_assigned, R.string.error_assign_failed);
    }

    public void changeStatus(String status, String reason) {
        detailAction(callback -> serviceRequestRepository.changeStatus(requestId, status, reason, callback),
                R.string.status_changed, R.string.error_status_change_failed);
    }

    public void submitQuotation(BigDecimal amount, String currency, String notes) {
        detailAction(callback -> serviceRequestRepository.submitQuotation(
                        requestId, amount, currency, notes, callback),
                R.string.quotation_submitted, R.string.error_quotation_submit_failed);
    }

    // Every mutating action returns the updated request, so they all share one shape: fire, then
    // either publish the new detail or surface the error.
    private interface DetailCall {
        void enqueue(Callback<ServiceRequestDetail> callback);
    }

    private void detailAction(DetailCall action, int successRes, int failureRes) {

        action.enqueue(new Callback<ServiceRequestDetail>() {

            @Override
            public void onResponse(Call<ServiceRequestDetail> call,
                                   Response<ServiceRequestDetail> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    apiError.setValue(new Event<>(ApiErrors.describe(response,
                            getApplication().getString(failureRes))));
                    return;
                }

                message.setValue(new Event<>(successRes));
                detail.setValue(response.body());
                // The action usually moves the status, so the timeline is now stale.
                loadHistory();
            }

            @Override
            public void onFailure(Call<ServiceRequestDetail> call, Throwable t) {
                message.setValue(new Event<>(failureRes));
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
