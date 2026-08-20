package com.businessos.modules.servicedesk.kb;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KbArticleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String summary;

    @NotBlank(message = "Content is required")
    private String content;

    private boolean clientVisible;

    private String keywords;

    private Long categoryId;

    private Long relatedServiceId;
}
