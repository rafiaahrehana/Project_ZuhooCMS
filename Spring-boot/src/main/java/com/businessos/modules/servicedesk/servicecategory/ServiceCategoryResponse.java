package com.businessos.modules.servicedesk.servicecategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceCategoryResponse {
    private Long id;
    private String name;
    private String nameBn;
    private String description;
    private String iconUrl;
    private int sortOrder;
    private boolean active;
}
