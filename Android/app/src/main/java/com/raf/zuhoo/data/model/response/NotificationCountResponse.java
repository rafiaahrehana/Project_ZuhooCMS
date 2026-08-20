package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class NotificationCountResponse {

    @SerializedName("unreadCount")
    private long unreadCount;

    public long getUnreadCount() {
        return unreadCount;
    }
}
