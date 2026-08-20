package com.businessos.modules.servicedesk.servicereview;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceReviewResponse {
    private Long id;
    private int rating;
    private String comment;
    private boolean published;
    private Long serviceRequestId;
    private Long hubServiceId;
    private String hubServiceName;
    private Long clientId;
    private String clientName;
    private LocalDateTime createdAt;
}
