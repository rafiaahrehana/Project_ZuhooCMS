package com.raf.zuhoo.ui.leave;

import android.content.Context;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.LeaveType;

// Localized labels for the fixed LeaveType wire constants — used both by the create-request
// spinner and by list rows showing an existing request's type.
public final class LeaveTypeLabels {

    private LeaveTypeLabels() {
    }

    public static String labelFor(Context context, String leaveType) {

        if (leaveType == null) {
            return "";
        }

        switch (leaveType) {
            case LeaveType.ANNUAL:
                return context.getString(R.string.leave_type_annual);
            case LeaveType.SICK:
                return context.getString(R.string.leave_type_sick);
            case LeaveType.CASUAL:
                return context.getString(R.string.leave_type_casual);
            case LeaveType.MATERNITY:
                return context.getString(R.string.leave_type_maternity);
            case LeaveType.PATERNITY:
                return context.getString(R.string.leave_type_paternity);
            case LeaveType.UNPAID:
                return context.getString(R.string.leave_type_unpaid);
            case LeaveType.COMPENSATORY:
                return context.getString(R.string.leave_type_compensatory);
            case LeaveType.EMERGENCY:
                return context.getString(R.string.leave_type_emergency);
            case LeaveType.OTHER:
                return context.getString(R.string.leave_type_other);
            default:
                return leaveType;
        }
    }

    /** Labels in the same order as LeaveType.VALUES, for the create-request spinner. */
    public static String[] allLabels(Context context) {
        String[] labels = new String[LeaveType.VALUES.length];
        for (int i = 0; i < LeaveType.VALUES.length; i++) {
            labels[i] = labelFor(context, LeaveType.VALUES[i]);
        }
        return labels;
    }
}
