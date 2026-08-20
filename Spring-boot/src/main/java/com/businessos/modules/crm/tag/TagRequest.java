package com.businessos.modules.crm.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(max = 60, message = "Tag name must not exceed 60 characters")
    private String name;

    @NotBlank(message = "Tag color is required")
    @Size(max = 20, message = "Tag color must not exceed 20 characters")
    private String color;
}
