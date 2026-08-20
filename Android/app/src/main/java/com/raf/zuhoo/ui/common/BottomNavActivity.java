package com.raf.zuhoo.ui.common;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.raf.zuhoo.R;
import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.ui.account.AccountActivity;
import com.raf.zuhoo.ui.catalog.CatalogActivity;
import com.raf.zuhoo.ui.dashboard.DashboardActivity;
import com.raf.zuhoo.ui.invoice.InvoiceListActivity;
import com.raf.zuhoo.ui.servicerequest.ServiceRequestListActivity;
import com.raf.zuhoo.ui.servicerequest.StaffServiceRequestListActivity;
import com.raf.zuhoo.ui.support.SupportTicketListActivity;

/**
 * Gives the top-level screens a shared bottom navigation bar.
 *
 * Implemented by wrapping setContentView rather than editing six layouts: each screen keeps the
 * root it already has (some are ScrollView, some LinearLayout) and gets dropped into a frame
 * above the nav bar. That also means a screen opts in purely by extending this class.
 *
 * The app is Activity-per-screen rather than Fragment-based, so switching tabs starts an
 * Activity. REORDER_TO_FRONT keeps one instance of each tab rather than stacking duplicates as
 * the user moves between them.
 */
public abstract class BottomNavActivity extends AppCompatActivity {

    /** Menu item id for this screen's tab, or 0 to show the bar with nothing selected. */
    protected abstract int selectedNavItemId();

    private BottomNavigationView bottomNav;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        setContentView(LayoutInflater.from(this).inflate(layoutResID, null));
    }

    @Override
    public void setContentView(View view) {
        setContentView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {

        View shell = LayoutInflater.from(this).inflate(R.layout.shell_bottom_nav, null);

        FrameLayout content = shell.findViewById(R.id.contentFrame);
        content.addView(view, params);

        bottomNav = shell.findViewById(R.id.bottomNav);

        super.setContentView(shell);

        setUpNav();
    }

    private void setUpNav() {

        String role = ZuhooApplication.graph().tokenManager().getRole();
        boolean staff = Role.COMPANY_OWNER.equals(role) || Role.EMPLOYEE.equals(role);

        bottomNav.inflateMenu(staff ? R.menu.bottom_nav_staff : R.menu.bottom_nav_client);

        int selected = selectedNavItemId();
        if (selected != 0 && bottomNav.getMenu().findItem(selected) != null) {
            bottomNav.setSelectedItemId(selected);
        }

        bottomNav.setOnItemSelectedListener(item -> {

            // Already here — don't relaunch the screen the user is looking at.
            if (item.getItemId() == selectedNavItemId()) {
                return true;
            }

            Intent intent = intentFor(item.getItemId(), staff);

            if (intent == null) {
                return false;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            // No transition animation — tab switches should feel like the same screen changing,
            // not a push onto a stack.
            overridePendingTransition(0, 0);

            return true;
        });
    }

    @Nullable
    private Intent intentFor(int itemId, boolean staff) {

        if (itemId == R.id.nav_home) {
            return new Intent(this, DashboardActivity.class);
        }

        if (itemId == R.id.nav_requests) {
            if (!staff) {
                return new Intent(this, ServiceRequestListActivity.class);
            }
            Intent intent = new Intent(this, StaffServiceRequestListActivity.class);
            intent.putExtra(StaffServiceRequestListActivity.EXTRA_MODE,
                    StaffServiceRequestListActivity.MODE_ALL);
            return intent;
        }

        if (itemId == R.id.nav_catalog) {
            return new Intent(this, CatalogActivity.class);
        }

        if (itemId == R.id.nav_billing) {
            return new Intent(this, InvoiceListActivity.class);
        }

        if (itemId == R.id.nav_support) {
            return new Intent(this, SupportTicketListActivity.class);
        }

        if (itemId == R.id.nav_account) {
            return new Intent(this, AccountActivity.class);
        }

        return null;
    }
}
