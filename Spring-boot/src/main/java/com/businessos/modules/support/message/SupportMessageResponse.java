package com.businessos.modules.support.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageResponse {
    private Long id;
    private Long ticketId;
    private Long sentById;
    private String sentByName;
    private String message;
    private String messageType;
    private boolean isInternal;
    private String attachmentUrl;
    private String attachmentFileName;
    private Long attachmentSize;
    private boolean isResolution;
    private LocalDateTime createdAt;
}
