package com.businessos.modules.servicedesk.kb;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KbArticleResponse {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private KbArticleStatus status;
    private boolean clientVisible;
    private String keywords;
    private int viewCount;
    private int helpfulCount;
    private LocalDateTime publishedAt;
    private Long categoryId;
    private String categoryName;
    private Long relatedServiceId;
    private String relatedServiceName;
    private String authorName;
    private LocalDateTime createdAt;
}
