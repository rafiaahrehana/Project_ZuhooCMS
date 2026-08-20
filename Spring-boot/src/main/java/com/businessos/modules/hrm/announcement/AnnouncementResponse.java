package com.businessos.modules.hrm.announcement;

import com.businessos.enums.AnnouncementAudience;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String body;
    private AnnouncementAudience audience;
    private Long targetDepartmentId;
    private String targetDepartmentName;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime scheduledAt;
    private boolean published;
    private boolean notifyAll;
    private int priority;
    private String attachmentUrl;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
