package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

// Trimmed the same way as FinanceDashboardResponse — top-line headcount/attendance figures only,
// skipping the trend/pipeline/joiner/upcoming lists the web dashboard renders as charts.
public class HrDashboardResponse {

    @SerializedName("totalEmployees")
    private long totalEmployees;
    @SerializedName("presentToday")
    private long presentToday;
    @SerializedName("onLeaveToday")
    private long onLeaveToday;
    @SerializedName("absentToday")
    private long absentToday;
    @SerializedName("openPositions")
    private long openPositions;
    @SerializedName("leaveSummary")
    private LeaveSummary leaveSummary;

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public long getPresentToday() {
        return presentToday;
    }

    public long getOnLeaveToday() {
        return onLeaveToday;
    }

    public long getAbsentToday() {
        return absentToday;
    }

    public long getOpenPositions() {
        return openPositions;
    }

    public long getPendingLeaveApprovals() {
        return leaveSummary == null ? 0 : leaveSummary.pending;
    }

    public static class LeaveSummary {
        @SerializedName("pending")
        private long pending;
    }
}
