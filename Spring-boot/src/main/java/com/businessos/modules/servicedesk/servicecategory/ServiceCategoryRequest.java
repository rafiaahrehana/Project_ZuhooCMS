package com.businessos.modules.servicedesk.servicecategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 100)
    private String nameBn;

    private String description;
    private String iconUrl;
    private int sortOrder;
}
