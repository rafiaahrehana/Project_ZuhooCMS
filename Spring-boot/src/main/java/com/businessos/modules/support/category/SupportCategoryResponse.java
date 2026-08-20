package com.businessos.modules.support.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportCategoryResponse {
    private Long id;
    private String categoryName;
    private String description;
    private boolean active;
    private String icon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
