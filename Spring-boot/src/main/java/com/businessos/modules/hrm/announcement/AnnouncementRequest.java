package com.businessos.modules.hrm.announcement;

import com.businessos.enums.AnnouncementAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;
    @NotBlank(message = "Body is required")
    private String body;
    private AnnouncementAudience audience;
    private Long targetDepartmentId;
    private LocalDateTime expiresAt;
    private LocalDateTime scheduledAt;
    private boolean notifyAll;
    private Integer priority;
    private String attachmentUrl;
}
