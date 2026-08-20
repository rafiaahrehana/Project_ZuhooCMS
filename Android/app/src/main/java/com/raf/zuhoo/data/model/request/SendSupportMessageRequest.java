package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class SendSupportMessageRequest {

    @SerializedName("ticketId")
    private final Long ticketId;
    @SerializedName("message")
    private final String message;
    @SerializedName("attachmentUrl")
    private final String attachmentUrl;
    @SerializedName("attachmentFileName")
    private final String attachmentFileName;

    public SendSupportMessageRequest(Long ticketId, String message,
                                     String attachmentUrl, String attachmentFileName) {
        this.ticketId = ticketId;
        this.message = message;
        this.attachmentUrl = attachmentUrl;
        this.attachmentFileName = attachmentFileName;
    }

    // isInternal is deliberately absent: it exists for platform-staff notes, and there is no
    // legitimate case for a company user talking to platform support to set it.
    //
    // sentByUserId is likewise absent — the backend derives the sender from the JWT and ignores
    // any client-supplied value.
}
