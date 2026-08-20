package com.businessos.shared.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLocationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Type is required")
    private LocationType type;

    // Required for LEVEL1 nodes (the node's country); ignored otherwise.
    private Long countryId;

    // Required for LEVEL2-LEVEL4 nodes (the node one level up); ignored for LEVEL1.
    private Long parentId;
}
