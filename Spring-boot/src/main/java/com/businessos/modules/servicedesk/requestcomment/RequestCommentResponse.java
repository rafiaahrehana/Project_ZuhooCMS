package com.businessos.modules.servicedesk.requestcomment;

import com.businessos.enums.CommentVisibility;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RequestCommentResponse {
    private Long id;
    private String content;
    private CommentVisibility visibility;
    private String attachmentUrl;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
}
