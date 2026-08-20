package com.businessos.modules.servicedesk.requestcomment;

import com.businessos.enums.CommentVisibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentRequest {

    @NotBlank(message = "Comment content is required")
    private String content;

    private CommentVisibility visibility;

    private String attachmentUrl;
}
