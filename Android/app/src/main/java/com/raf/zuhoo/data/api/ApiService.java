package com.raf.zuhoo.data.api;

import com.raf.zuhoo.data.model.request.AddCommentRequest;
import com.raf.zuhoo.data.model.request.AttendanceCheckInRequest;
import com.raf.zuhoo.data.model.request.AttendanceCheckOutRequest;
import com.raf.zuhoo.data.model.request.ChangePasswordRequest;
import com.raf.zuhoo.data.model.request.ChangeRequestStatusRequest;
import com.raf.zuhoo.data.model.request.CreateServiceRequestRequest;
import com.raf.zuhoo.data.model.request.CreateSupportTicketRequest;
import com.raf.zuhoo.data.model.request.ForgotPasswordRequest;
import com.raf.zuhoo.data.model.request.ResendVerificationRequest;
import com.raf.zuhoo.data.model.request.ResetPasswordRequest;
import com.raf.zuhoo.data.model.request.VerifyEmailRequest;
import com.raf.zuhoo.data.model.request.InitiatePaymentRequest;
import com.raf.zuhoo.data.model.request.GoogleAuthRequest;
import com.raf.zuhoo.data.model.request.GoogleRegisterRequest;
import com.raf.zuhoo.data.model.request.LoginRequest;
import com.raf.zuhoo.data.model.request.PublicClientRegisterRequest;
import com.raf.zuhoo.data.model.request.RefreshTokenRequest;
import com.raf.zuhoo.data.model.request.RegisterDeviceTokenRequest;
import com.raf.zuhoo.data.model.request.RegisterRequest;
import com.raf.zuhoo.data.model.request.RejectQuotationRequest;
import com.raf.zuhoo.data.model.request.SendSupportMessageRequest;
import com.raf.zuhoo.data.model.request.ServiceReviewRequest;
import com.raf.zuhoo.data.model.request.SubmitQuotationRequest;
import com.raf.zuhoo.data.model.request.SubscribeRequest;
import com.raf.zuhoo.data.model.request.UpdateMyClientProfileRequest;
import com.raf.zuhoo.data.model.request.CreateExpenseRequest;
import com.raf.zuhoo.data.model.request.CreateLeaveRequestRequest;
import com.raf.zuhoo.data.model.request.LogTimesheetRequest;
import com.raf.zuhoo.data.model.request.ReviewLeaveRequestRequest;
import com.raf.zuhoo.data.model.request.UpdateLeadStatusRequest;
import com.raf.zuhoo.data.model.request.UpdateAttendanceLocationSettingsRequest;
import com.raf.zuhoo.data.model.request.UpdateNotificationPreferenceRequest;
import com.raf.zuhoo.data.model.request.UpdateUserProfileRequest;
import com.raf.zuhoo.data.model.response.AttendanceLocationSettingsResponse;
import com.raf.zuhoo.data.model.response.AttendanceResponse;
import com.raf.zuhoo.data.model.response.ClientResponse;
import com.raf.zuhoo.data.model.response.ClientSummaryResponse;
import com.raf.zuhoo.data.model.response.AnnouncementResponse;
import com.raf.zuhoo.data.model.response.ExpenseResponse;
import com.raf.zuhoo.data.model.response.FinanceDashboardResponse;
import com.raf.zuhoo.data.model.response.HolidayResponse;
import com.raf.zuhoo.data.model.response.GlobalSearchResponse;
import com.raf.zuhoo.data.model.response.HrDashboardResponse;
import com.raf.zuhoo.data.model.response.LeadResponse;
import com.raf.zuhoo.data.model.response.TimesheetResponse;
import com.raf.zuhoo.data.model.response.WalletResponse;
import com.raf.zuhoo.data.model.response.WalletTransactionResponse;
import com.raf.zuhoo.data.model.response.LeaveBalanceResponse;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.model.response.PayrollResponse;
import com.raf.zuhoo.data.model.response.CompanyPublicResponse;
import com.raf.zuhoo.data.model.response.CompanyServiceResponse;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.GoogleSignInResponse;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.data.model.response.JwtResponse;
import com.raf.zuhoo.data.model.response.KbArticleResponse;
import com.raf.zuhoo.data.model.response.LoginResponse;
import com.raf.zuhoo.data.model.response.NotificationCountResponse;
import com.raf.zuhoo.data.model.response.NotificationPreferenceResponse;
import com.raf.zuhoo.data.model.response.NotificationResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.PaymentReceiptSummary;
import com.raf.zuhoo.data.model.response.RequestComment;
import com.raf.zuhoo.data.model.response.RequestStatusHistory;
import com.raf.zuhoo.data.model.response.ServiceCategoryResponse;
import com.raf.zuhoo.data.model.response.ServiceFormField;
import com.raf.zuhoo.data.model.response.ServicePackageResponse;
import com.raf.zuhoo.data.model.response.ServiceRequestDetail;
import com.raf.zuhoo.data.model.response.ServiceRequestSummary;
import com.raf.zuhoo.data.model.response.ServiceReviewResponse;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;
import com.raf.zuhoo.data.model.response.SupportCategoryResponse;
import com.raf.zuhoo.data.model.response.SupportMessageResponse;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.data.model.response.UserProfileResponse;
import com.raf.zuhoo.data.model.response.UserResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<UserResponse> register(@Body RegisterRequest request);

    // Called only by TokenAuthenticator, on its own Retrofit instance (see TokenRefreshClient) —
    // never through the authenticated client, or a failing refresh would recurse.
    @POST("api/auth/refresh")
    Call<JwtResponse> refresh(@Body RefreshTokenRequest request);

    // Google sign-in. Either logs in (registered=true) or reports that this Google account has
    // no user yet, in which case the app collects a company and calls the register endpoint.
    @POST("api/auth/google")
    Call<GoogleSignInResponse> googleSignIn(@Body GoogleAuthRequest request);

    @POST("api/auth/google/register")
    Call<LoginResponse> googleRegister(@Body GoogleRegisterRequest request);

    @GET("api/companies/public/list")
    Call<List<CompanyPublicResponse>> getPublicCompanies();

    @POST("api/clients/public/register")
    Call<ClientResponse> registerClient(@Body PublicClientRegisterRequest request);

    @GET("api/dashboard/client-summary")
    Call<ClientSummaryResponse> getClientSummary();

    @GET("api/service-requests/my")
    Call<PageResponse<ServiceRequestSummary>> getMyServiceRequests();

    @GET("api/company/finance/invoices/me")
    Call<PageResponse<InvoiceSummary>> getMyInvoices();

    @GET("api/packages/subscriptions/my")
    Call<PageResponse<SubscriptionSummary>> getMySubscriptions();

    @GET("api/service-requests/{id}")
    Call<ServiceRequestDetail> getServiceRequest(@Path("id") Long id);

    @POST("api/service-requests")
    Call<ServiceRequestDetail> createServiceRequest(@Body CreateServiceRequestRequest request);

    @PATCH("api/service-requests/{id}/cancel")
    Call<ResponseBody> cancelServiceRequest(@Path("id") Long id);

    @GET("api/service-requests/{id}/history")
    Call<List<RequestStatusHistory>> getRequestHistory(@Path("id") Long id);

    @GET("api/service-requests/{id}/comments")
    Call<PageResponse<RequestComment>> getComments(@Path("id") Long id);

    @POST("api/service-requests/{id}/comments")
    Call<RequestComment> addComment(@Path("id") Long id, @Body AddCommentRequest request);

    @GET("api/services/active")
    Call<List<CompanyServiceResponse>> getActiveServices();

    // Custom fields an admin configured for this service — rendered into the create-request
    // form and submitted back in formData.
    @GET("api/v1/services/{serviceId}/form-fields")
    Call<List<ServiceFormField>> getServiceFormFields(@Path("serviceId") Long serviceId);

    @POST("api/service-requests/{id}/quotation/accept")
    Call<ServiceRequestDetail> acceptQuotation(@Path("id") Long id);

    @POST("api/service-requests/{id}/quotation/reject")
    Call<ServiceRequestDetail> rejectQuotation(@Path("id") Long id, @Body RejectQuotationRequest request);

    @GET("api/company/finance/invoices/{id}")
    Call<InvoiceSummary> getInvoice(@Path("id") Long id);

    @GET("api/company/finance/invoices/{id}/pdf")
    Call<ResponseBody> downloadInvoicePdf(@Path("id") Long id);

    @GET("api/company/finance/payment-receipts/me")
    Call<PageResponse<PaymentReceiptSummary>> getMyPaymentReceipts();

    @POST("api/payments/sslcommerz/initiate")
    Call<Map<String, String>> initiatePayment(@Body InitiatePaymentRequest request);

    @GET("api/service-categories")
    Call<List<ServiceCategoryResponse>> getServiceCategories();

    @GET("api/packages/active")
    Call<List<ServicePackageResponse>> getActivePackages();

    @POST("api/packages/subscribe")
    Call<SubscriptionSummary> subscribe(@Body SubscribeRequest request);

    @PATCH("api/packages/subscriptions/{id}/cancel")
    Call<ResponseBody> cancelSubscription(@Path("id") Long id, @Query("reason") String reason);

    @POST("api/reviews")
    Call<ServiceReviewResponse> submitReview(@Body ServiceReviewRequest request);

    @GET("api/service-requests")
    Call<PageResponse<ServiceRequestSummary>> getAllServiceRequests(@Query("status") String status);

    @GET("api/service-requests/assigned-to-me")
    Call<PageResponse<ServiceRequestSummary>> getAssignedToMe();

    @GET("api/employees")
    Call<PageResponse<EmployeeResponse>> getEmployees();

    @PATCH("api/service-requests/{id}/assign/{employeeId}")
    Call<ServiceRequestDetail> assignServiceRequest(@Path("id") Long id, @Path("employeeId") Long employeeId);

    @PATCH("api/service-requests/{id}/status")
    Call<ServiceRequestDetail> changeRequestStatus(@Path("id") Long id, @Body ChangeRequestStatusRequest request);

    @POST("api/service-requests/{id}/quotation")
    Call<ServiceRequestDetail> submitQuotation(@Path("id") Long id, @Body SubmitQuotationRequest request);

    @GET("api/support/categories/active")
    Call<List<SupportCategoryResponse>> getSupportCategories();

    @POST("api/v1/support/tickets")
    Call<SupportTicketResponse> createSupportTicket(@Body CreateSupportTicketRequest request);

    @GET("api/v1/support/tickets/my-tickets")
    Call<PageResponse<SupportTicketResponse>> getMySupportTickets();

    @GET("api/v1/support/tickets/{id}")
    Call<SupportTicketResponse> getSupportTicket(@Path("id") Long id);

    @GET("api/v1/support/messages/ticket/{id}/external")
    Call<List<SupportMessageResponse>> getSupportMessages(@Path("id") Long id);

    @POST("api/v1/support/messages")
    Call<SupportMessageResponse> sendSupportMessage(@Body SendSupportMessageRequest request);

    @POST("api/v1/support/tickets/{id}/satisfaction")
    Call<ResponseBody> submitSatisfaction(@Path("id") Long id, @Query("rating") int rating,
                                         @Query("feedback") String feedback);

    @POST("api/auth/logout")
    Call<ResponseBody> logout(@Body RefreshTokenRequest request);

    @POST("api/auth/change-password")
    Call<ResponseBody> changePassword(@Body ChangePasswordRequest request);

    // These four return a bare String, not the usual JSON envelope — hence ResponseBody.
    // Deserialising into a POJO would blow up in Gson.
    @POST("api/auth/forgot-password")
    Call<ResponseBody> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/reset-password")
    Call<ResponseBody> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/auth/verify-email")
    Call<ResponseBody> verifyEmail(@Body VerifyEmailRequest request);

    @POST("api/auth/resend-verification")
    Call<ResponseBody> resendVerification(@Body ResendVerificationRequest request);

    @GET("api/clients/me")
    Call<ClientResponse> getMyClientProfile();

    @PATCH("api/clients/me")
    Call<ClientResponse> updateMyClientProfile(@Body UpdateMyClientProfileRequest request);

    @GET("api/users/profile")
    Call<UserProfileResponse> getUserProfile();

    @PATCH("api/users/profile")
    Call<UserProfileResponse> updateUserProfile(@Body UpdateUserProfileRequest request);

    @GET("api/notifications")
    Call<PageResponse<NotificationResponse>> getNotifications(@Query("unreadOnly") boolean unreadOnly);

    @GET("api/notifications/count")
    Call<NotificationCountResponse> getNotificationCount();

    @PATCH("api/notifications/{id}/read")
    Call<ResponseBody> markNotificationRead(@Path("id") Long id);

    @PATCH("api/notifications/read-all")
    Call<ResponseBody> markAllNotificationsRead();

    // The backend derives the owner from the JWT — never send a userId.
    @POST("api/notifications/device-tokens")
    Call<ResponseBody> registerDeviceToken(@Body RegisterDeviceTokenRequest request);

    @DELETE("api/notifications/device-tokens/{token}")
    Call<ResponseBody> unregisterDeviceToken(@Path("token") String token);

    @GET("api/notification-preferences")
    Call<NotificationPreferenceResponse> getNotificationPreferences();

    @PUT("api/notification-preferences")
    Call<NotificationPreferenceResponse> updateNotificationPreferences(
            @Body UpdateNotificationPreferenceRequest request);

    @GET("api/kb/articles")
    Call<PageResponse<KbArticleResponse>> getKbArticles(@Query("keyword") String keyword);

    @GET("api/kb/articles/{id}")
    Call<KbArticleResponse> getKbArticle(@Path("id") Long id);

    @POST("api/kb/articles/{id}/helpful")
    Call<ResponseBody> markKbArticleHelpful(@Path("id") Long id);

    // Two-step attach: upload first to get a URL, then send that URL as the attachmentUrl on a
    // comment or support message. Response is {fileName, fileUrl, message}. 10 MB cap server-side.
    @Multipart
    @POST("api/upload")
    Call<Map<String, String>> uploadFile(@Part MultipartBody.Part file);

    @GET("api/company/attendance/my/today")
    Call<AttendanceResponse> getMyTodayAttendance();

    @POST("api/company/attendance/check-in")
    Call<AttendanceResponse> checkInAttendance(@Body AttendanceCheckInRequest request);

    @POST("api/company/attendance/{id}/check-out")
    Call<AttendanceResponse> checkOutAttendance(@Path("id") Long id, @Body AttendanceCheckOutRequest request);

    @GET("api/hr/attendance-location-settings")
    Call<AttendanceLocationSettingsResponse> getAttendanceLocationSettings();

    @PUT("api/hr/attendance-location-settings")
    Call<AttendanceLocationSettingsResponse> updateAttendanceLocationSettings(
            @Body UpdateAttendanceLocationSettingsRequest request);

    @GET("api/hr/leaves/my")
    Call<PageResponse<LeaveRequestResponse>> getMyLeaveRequests();

    @POST("api/hr/leaves")
    Call<LeaveRequestResponse> createLeaveRequest(@Body CreateLeaveRequestRequest request);

    @PATCH("api/hr/leaves/{id}/cancel")
    Call<ResponseBody> cancelLeaveRequest(@Path("id") Long id);

    @GET("api/hr/leaves")
    Call<PageResponse<LeaveRequestResponse>> getLeaveRequestsByStatus(@Query("status") String status);

    @PATCH("api/hr/leaves/{id}/review")
    Call<LeaveRequestResponse> reviewLeaveRequest(@Path("id") Long id, @Body ReviewLeaveRequestRequest request);

    @GET("api/hr/leave-balances/my")
    Call<List<LeaveBalanceResponse>> getMyLeaveBalances();

    @GET("api/employees/me")
    Call<EmployeeResponse> getMyEmployeeProfile();

    @GET("api/hr/payroll/employee/{employeeId}")
    Call<PageResponse<PayrollResponse>> getMyPayslips(@Path("employeeId") Long employeeId);

    @GET("api/hr/payroll/{id}/payslip")
    Call<ResponseBody> downloadPayslipPdf(@Path("id") Long id);

    @GET("api/announcements/active")
    Call<List<AnnouncementResponse>> getActiveAnnouncements();

    @GET("api/hr/holidays/current-year")
    Call<List<HolidayResponse>> getCurrentYearHolidays();

    @GET("api/company/finance/expenses/my-expenses")
    Call<PageResponse<ExpenseResponse>> getMyExpenses();

    @POST("api/company/finance/expenses")
    Call<ExpenseResponse> createExpense(@Body CreateExpenseRequest request);

    @GET("api/company/finance/expenses/status/{status}")
    Call<PageResponse<ExpenseResponse>> getExpensesByStatus(@Path("status") String status);

    // notes/reason travel as query params server-side, not a JSON body.
    @POST("api/company/finance/expenses/{id}/approve")
    Call<ResponseBody> approveExpense(@Path("id") Long id, @Query("notes") String notes);

    @POST("api/company/finance/expenses/{id}/reject")
    Call<ResponseBody> rejectExpense(@Path("id") Long id, @Query("reason") String reason);

    @GET("api/finance/dashboard")
    Call<FinanceDashboardResponse> getFinanceDashboard();

    @GET("api/hr/dashboard/summary")
    Call<HrDashboardResponse> getHrDashboard();

    @GET("api/crm/leads/my")
    Call<PageResponse<LeadResponse>> getMyLeads();

    @PATCH("api/crm/leads/{id}")
    Call<LeadResponse> updateLeadStatus(@Path("id") Long id, @Body UpdateLeadStatusRequest request);

    @GET("api/search")
    Call<GlobalSearchResponse> search(@Query("q") String query);

    @GET("api/wallet")
    Call<WalletResponse> getWallet();

    @GET("api/wallet/transactions")
    Call<PageResponse<WalletTransactionResponse>> getWalletTransactions();

    @GET("api/hr/timesheets/my")
    Call<PageResponse<TimesheetResponse>> getMyTimesheets();

    @POST("api/hr/timesheets")
    Call<TimesheetResponse> logTimesheet(@Body LogTimesheetRequest request);
}
