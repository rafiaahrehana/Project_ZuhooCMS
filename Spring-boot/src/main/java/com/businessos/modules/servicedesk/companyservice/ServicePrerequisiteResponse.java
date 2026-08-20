package com.businessos.modules.servicedesk.companyservice;

import lombok.Data;

@Data
public class ServicePrerequisiteResponse {
    private Long id;
    private Long serviceId;
    private Long prerequisiteServiceId;
    private String prerequisiteServiceName;
    private boolean mandatory;
    private String message;
}
