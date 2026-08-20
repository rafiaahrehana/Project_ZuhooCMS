package com.businessos.modules.support.contextswitch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportContextSwitchResponse {
    private Long id;
    private Long supportAgentId;
    private String supportAgentName;
    private Long viewedCompanyId;
    private String viewedCompanyName;
    private LocalDateTime switchedInTime;
    private LocalDateTime switchedOutTime;
    private String purpose;
    private String ipAddress;
    private String userAgent;
    private boolean stillActive;
}
