package com.businessos.modules.support.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAuditLogResponse {
    private Long id;
    private Long companyId;
    private Long actionByUserId;
    private String actionByUserName;
    private String actionType;
    private Long resourceId;
    private String resourceType;
    private String description;
    private String changes;
    private String ipAddress;
    private String userAgent;
    private Long contextSwitchToCompanyId;
    private String contextSwitchToCompanyName;
    private LocalDateTime createdAt;
}
