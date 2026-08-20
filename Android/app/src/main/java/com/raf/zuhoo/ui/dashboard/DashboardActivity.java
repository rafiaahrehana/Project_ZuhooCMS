package com.raf.zuhoo.ui.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.messaging.FirebaseMessaging;
import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.local.PushTokenStore;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.data.repository.DeviceTokenRepository;
import com.raf.zuhoo.databinding.ActivityDashboardBinding;
import com.raf.zuhoo.ui.account.AccountActivity;
import com.raf.zuhoo.ui.attendance.CheckInActivity;
import com.raf.zuhoo.ui.auth.LoginActivity;
import com.raf.zuhoo.ui.catalog.CatalogActivity;
import com.raf.zuhoo.ui.crm.LeadListActivity;
import com.raf.zuhoo.ui.expense.ExpenseApprovalListActivity;
import com.raf.zuhoo.ui.expense.ExpenseListActivity;
import com.raf.zuhoo.ui.leave.LeaveApprovalListActivity;
import com.raf.zuhoo.ui.leave.LeaveRequestListActivity;
import com.raf.zuhoo.ui.noticeboard.NoticeBoardActivity;
import com.raf.zuhoo.ui.search.SearchActivity;
import com.raf.zuhoo.ui.timesheet.TimesheetListActivity;
import com.raf.zuhoo.ui.payroll.PayslipListActivity;
import com.raf.zuhoo.ui.catalog.PackageListActivity;
import com.raf.zuhoo.ui.invoice.InvoiceListActivity;
import com.raf.zuhoo.ui.notification.NotificationListActivity;
import com.raf.zuhoo.ui.servicerequest.ServiceRequestListActivity;
import com.raf.zuhoo.ui.servicerequest.StaffServiceRequestListActivity;
import com.raf.zuhoo.ui.support.SupportTicketListActivity;

public class DashboardActivity extends BottomNavActivity {

    private ActivityDashboardBinding binding;
    private DashboardViewModel viewModel;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        if (!tokenManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        bindSession();
        wireNavigation();
        observeViewModel();
        setUpPush();

        viewModel.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Idempotent: subscribes once, then re-reads the count on every return to this screen
        // (e.g. after marking notifications read elsewhere).
        ZuhooApplication.graph().notificationCenter().start();
    }

    /**
     * Push registration is done here rather than on the login screen because it needs a live
     * session, and this is the first screen every signed-in path lands on — including a resumed
     * session that skipped the login form entirely.
     */
    private void setUpPush() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Asked once; if declined, everything still works minus the system notifications —
            // the in-app notification list is unaffected.
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }

        DeviceTokenRepository deviceTokenRepository = new DeviceTokenRepository(this);
        PushTokenStore pushTokenStore = new PushTokenStore(this);

        // onNewToken only fires when the token *changes*, so on an ordinary launch we have to ask
        // for the current one — otherwise a device that got its token before logging in would
        // never register.
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {

            if (!task.isSuccessful() || task.getResult() == null) {
                // No Firebase project configured yet (the placeholder google-services.json), or
                // no Play Services. Not an error the user can act on.
                return;
            }

            String token = task.getResult();

            if (!token.equals(pushTokenStore.getToken())) {
                pushTokenStore.setToken(token);
            }

            deviceTokenRepository.registerIfNeeded();
        });
    }

    private void wireNavigation() {

        binding.btnAccount.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));
        binding.btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationListActivity.class)));
        binding.btnViewRequests.setOnClickListener(v ->
                startActivity(new Intent(this, ServiceRequestListActivity.class)));
        binding.btnViewInvoices.setOnClickListener(v ->
                startActivity(new Intent(this, InvoiceListActivity.class)));
        binding.btnBrowseCatalog.setOnClickListener(v ->
                startActivity(new Intent(this, CatalogActivity.class)));
        binding.btnViewPackages.setOnClickListener(v ->
                startActivity(new Intent(this, PackageListActivity.class)));

        binding.btnCheckIn.setOnClickListener(v ->
                startActivity(new Intent(this, CheckInActivity.class)));
        binding.btnLeaveRequests.setOnClickListener(v ->
                startActivity(new Intent(this, LeaveRequestListActivity.class)));
        binding.btnLeaveApprovals.setOnClickListener(v ->
                startActivity(new Intent(this, LeaveApprovalListActivity.class)));
        binding.btnPayslips.setOnClickListener(v ->
                startActivity(new Intent(this, PayslipListActivity.class)));
        binding.btnNoticeBoard.setOnClickListener(v ->
                startActivity(new Intent(this, NoticeBoardActivity.class)));
        binding.btnExpenses.setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseListActivity.class)));
        binding.btnExpenseApprovals.setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseApprovalListActivity.class)));
        binding.btnMyLeads.setOnClickListener(v ->
                startActivity(new Intent(this, LeadListActivity.class)));
        binding.btnTimesheet.setOnClickListener(v ->
                startActivity(new Intent(this, TimesheetListActivity.class)));
        binding.btnSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        binding.btnStaffAllRequests.setOnClickListener(v ->
                openStaffRequests(StaffServiceRequestListActivity.MODE_ALL));
        binding.btnStaffAssignedRequests.setOnClickListener(v ->
                openStaffRequests(StaffServiceRequestListActivity.MODE_ASSIGNED_TO_ME));
        binding.btnStaffSupportTickets.setOnClickListener(v ->
                startActivity(new Intent(this, SupportTicketListActivity.class)));
    }

    private void openStaffRequests(String mode) {
        Intent intent = new Intent(this, StaffServiceRequestListActivity.class);
        intent.putExtra(StaffServiceRequestListActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    private void bindSession() {

        binding.welcomeText.setText(
                getString(R.string.dashboard_welcome, tokenManager.getFirstName()));

        binding.roleBadge.setText(getString(R.string.dashboard_role_badge,
                tokenManager.getRole(), tokenManager.getCompanyId()));

        if (viewModel.isStaff()) {
            binding.staffStatsGrid.setVisibility(View.VISIBLE);
            binding.sectionRequestsHeader.setVisibility(View.VISIBLE);
            binding.btnStaffAllRequests.setVisibility(View.VISIBLE);
            binding.btnStaffAssignedRequests.setVisibility(View.VISIBLE);
            binding.btnStaffSupportTickets.setVisibility(View.VISIBLE);
            binding.sectionMyWorkHeader.setVisibility(View.VISIBLE);
            // Both EMPLOYEE and COMPANY_OWNER take leave, unlike check-in.
            binding.btnLeaveRequests.setVisibility(View.VISIBLE);
            binding.sectionApprovalsHeader.setVisibility(View.VISIBLE);
            binding.btnLeaveApprovals.setVisibility(View.VISIBLE);
            binding.btnPayslips.setVisibility(View.VISIBLE);
            binding.sectionManageHeader.setVisibility(View.VISIBLE);
            binding.btnNoticeBoard.setVisibility(View.VISIBLE);
            binding.btnExpenses.setVisibility(View.VISIBLE);
            binding.btnExpenseApprovals.setVisibility(View.VISIBLE);
            binding.btnMyLeads.setVisibility(View.VISIBLE);
            binding.btnTimesheet.setVisibility(View.VISIBLE);
            binding.btnSearch.setVisibility(View.VISIBLE);
            // Company owners manage the company rather than clocking in themselves — narrower
            // than isStaff(), which also covers them.
            if (Role.EMPLOYEE.equals(tokenManager.getRole())) {
                binding.btnCheckIn.setVisibility(View.VISIBLE);
            }
        } else {
            binding.statsGrid.setVisibility(View.VISIBLE);
            binding.sectionQuickActionsHeader.setVisibility(View.VISIBLE);
            binding.btnViewRequests.setVisibility(View.VISIBLE);
            binding.btnViewInvoices.setVisibility(View.VISIBLE);
            binding.btnBrowseCatalog.setVisibility(View.VISIBLE);
            binding.btnViewPackages.setVisibility(View.VISIBLE);
        }
    }

    private void observeViewModel() {

        viewModel.clientSummary().observe(this, summary -> {
            binding.statOpenRequestsValue.setText(String.valueOf(summary.getPendingRequests()));
            binding.statPendingQuotationsValue.setText(
                    String.valueOf(summary.getInProgressRequests()));
            binding.statUnpaidInvoicesValue.setText(String.valueOf(summary.getUnpaidInvoices()));
        });

        viewModel.activeSubscriptions().observe(this, count ->
                binding.statActiveSubscriptionsValue.setText(String.valueOf(count)));

        viewModel.allOpenRequests().observe(this, count ->
                binding.statAllOpenRequestsValue.setText(String.valueOf(count)));

        viewModel.assignedToMe().observe(this, count ->
                binding.statAssignedToMeValue.setText(String.valueOf(count)));

        viewModel.openTickets().observe(this, count ->
                binding.statOpenTicketsValue.setText(String.valueOf(count)));

        viewModel.statsError().observe(this, event -> {
            if (event.consume() != null) {
                Toast.makeText(this, R.string.error_dashboard_stats_failed, Toast.LENGTH_SHORT).show();
            }
        });

        ZuhooApplication.graph().notificationCenter().unreadCount().observe(this, count -> {
            boolean hasUnread = count != null && count > 0;
            binding.notificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
            if (hasUnread) {
                binding.notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected int selectedNavItemId() {
        return R.id.nav_home;
    }
}
