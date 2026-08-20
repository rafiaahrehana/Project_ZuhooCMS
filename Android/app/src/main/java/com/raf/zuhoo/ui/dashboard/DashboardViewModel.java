package com.raf.zuhoo.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.data.model.ServiceRequestStatus;
import com.raf.zuhoo.data.model.SubscriptionStatus;
import com.raf.zuhoo.data.model.TicketStatus;
import com.raf.zuhoo.data.model.response.ClientSummaryResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.ServiceRequestSummary;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.data.repository.DashboardRepository;
import com.raf.zuhoo.data.repository.ServiceRequestRepository;
import com.raf.zuhoo.data.repository.SubscriptionRepository;
import com.raf.zuhoo.data.repository.SupportTicketRepository;
import com.raf.zuhoo.ui.common.Event;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends AndroidViewModel {

    private final DashboardRepository dashboardRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SupportTicketRepository supportTicketRepository;

    private final boolean staff;

    private final MutableLiveData<ClientSummaryResponse> clientSummary = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeSubscriptions = new MutableLiveData<>();
    private final MutableLiveData<Integer> allOpenRequests = new MutableLiveData<>();
    private final MutableLiveData<Integer> assignedToMe = new MutableLiveData<>();
    private final MutableLiveData<Integer> openTickets = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> statsError = new MutableLiveData<>();

    private boolean started;
    private boolean errorReported;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        dashboardRepository = new DashboardRepository(application);
        serviceRequestRepository = new ServiceRequestRepository(application);
        subscriptionRepository = new SubscriptionRepository(application);
        supportTicketRepository = new SupportTicketRepository(application);

        String role = ZuhooApplication.graph().tokenManager().getRole();
        staff = Role.COMPANY_OWNER.equals(role) || Role.EMPLOYEE.equals(role);
    }

    public boolean isStaff() {
        return staff;
    }

    public LiveData<ClientSummaryResponse> clientSummary() {
        return clientSummary;
    }

    public LiveData<Integer> activeSubscriptions() {
        return activeSubscriptions;
    }

    public LiveData<Integer> allOpenRequests() {
        return allOpenRequests;
    }

    public LiveData<Integer> assignedToMe() {
        return assignedToMe;
    }

    public LiveData<Integer> openTickets() {
        return openTickets;
    }

    public LiveData<Event<Boolean>> statsError() {
        return statsError;
    }

    public void start() {

        if (started) {
            return;
        }

        started = true;

        if (staff) {
            loadStaffStats();
        } else {
            loadClientStats();
        }
    }

    private void loadClientStats() {

        // Three of the four cards come straight from the server-side summary. Counting a page of
        // /my instead — which is what this used to do — silently stops counting past the first
        // 20 rows, so a client with more history than that saw numbers that were simply wrong.
        dashboardRepository.getClientSummary(new Callback<ClientSummaryResponse>() {

            @Override
            public void onResponse(Call<ClientSummaryResponse> call,
                                   Response<ClientSummaryResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    reportStatsError();
                    return;
                }

                clientSummary.setValue(response.body());
            }

            @Override
            public void onFailure(Call<ClientSummaryResponse> call, Throwable t) {
                reportStatsError();
            }
        });

        // No summary field for subscriptions, so this one still counts a page. Subscriptions per
        // client are few enough that a first page covers them in practice.
        subscriptionRepository.getMySubscriptions(new Callback<PageResponse<SubscriptionSummary>>() {

            @Override
            public void onResponse(Call<PageResponse<SubscriptionSummary>> call,
                                   Response<PageResponse<SubscriptionSummary>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    reportStatsError();
                    return;
                }

                int active = 0;
                for (SubscriptionSummary subscription : response.body().getContent()) {
                    if (SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
                        active++;
                    }
                }

                activeSubscriptions.setValue(active);
            }

            @Override
            public void onFailure(Call<PageResponse<SubscriptionSummary>> call, Throwable t) {
                reportStatsError();
            }
        });
    }

    private void loadStaffStats() {

        serviceRequestRepository.getAllServiceRequests(null, openRequestCounter(allOpenRequests));
        serviceRequestRepository.getAssignedToMe(openRequestCounter(assignedToMe));

        supportTicketRepository.getMyTickets(new Callback<PageResponse<SupportTicketResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<SupportTicketResponse>> call,
                                   Response<PageResponse<SupportTicketResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    reportStatsError();
                    return;
                }

                int open = 0;
                for (SupportTicketResponse ticket : response.body().getContent()) {
                    if (TicketStatus.isOpen(ticket.getStatus())) {
                        open++;
                    }
                }

                openTickets.setValue(open);
            }

            @Override
            public void onFailure(Call<PageResponse<SupportTicketResponse>> call, Throwable t) {
                reportStatsError();
            }
        });
    }

    // There's no staff equivalent of the client summary endpoint, so these two still count a
    // page — the numbers are a first-page approximation for a busy company.
    private Callback<PageResponse<ServiceRequestSummary>> openRequestCounter(
            MutableLiveData<Integer> target) {

        return new Callback<PageResponse<ServiceRequestSummary>>() {

            @Override
            public void onResponse(Call<PageResponse<ServiceRequestSummary>> call,
                                   Response<PageResponse<ServiceRequestSummary>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    reportStatsError();
                    return;
                }

                int open = 0;
                for (ServiceRequestSummary request : response.body().getContent()) {
                    if (ServiceRequestStatus.isOpen(request.getStatus())) {
                        open++;
                    }
                }

                target.setValue(open);
            }

            @Override
            public void onFailure(Call<PageResponse<ServiceRequestSummary>> call, Throwable t) {
                reportStatsError();
            }
        };
    }

    // Several calls fan out at once; one toast is enough for the user to know the numbers are
    // stale.
    private void reportStatsError() {
        if (errorReported) {
            return;
        }
        errorReported = true;
        statsError.setValue(new Event<>(true));
    }
}
