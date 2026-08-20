package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class AddCommentRequest {

    // Matches CommentVisibility on the backend. Only CLIENT is ever sent from this app —
    // INTERNAL is a staff-to-staff note, and there's no composer for one here.
    public static final String VISIBILITY_CLIENT = "CLIENT";

    @SerializedName("content")
    private final String content;
    @SerializedName("visibility")
    private final String visibility;
    @SerializedName("attachmentUrl")
    private final String attachmentUrl;

    private AddCommentRequest(String content, String visibility, String attachmentUrl) {
        this.content = content;
        this.visibility = visibility;
        this.attachmentUrl = attachmentUrl;
    }

    // Staff comments MUST carry an explicit visibility. ServiceRequestServiceImpl.addComment()
    // defaults a null visibility per role — CLIENT for clients, but INTERNAL for staff — and
    // getComments() then filters INTERNAL out for the client. Leaving it null on the staff side
    // makes every reply invisible to the client it was written for.
    public static AddCommentRequest fromStaff(String content, String attachmentUrl) {
        return new AddCommentRequest(content, VISIBILITY_CLIENT, attachmentUrl);
    }

    // Clients can leave visibility null and take the server-side default, which is already CLIENT.
    public static AddCommentRequest fromClient(String content, String attachmentUrl) {
        return new AddCommentRequest(content, null, attachmentUrl);
    }
}
