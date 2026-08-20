package com.businessos.modules.support.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String categoryName;
    private String description;
    private boolean active = true;
    private String icon;
}
