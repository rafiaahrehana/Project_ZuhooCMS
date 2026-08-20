package com.businessos.modules.support.ticket;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicketRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private Long categoryId;

    @NotNull(message = "Priority is required")
    private TicketPriority priority;

    @Builder.Default
    private TicketSource source = TicketSource.PORTAL;

    private String attachmentUrl;
    private String attachmentFileName;
}
