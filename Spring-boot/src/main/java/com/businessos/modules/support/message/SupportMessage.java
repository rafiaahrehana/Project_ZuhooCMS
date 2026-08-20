package com.businessos.modules.support.message;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.support.ticket.SupportTicket;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "support_messages", indexes = {
        @Index(name = "idx_message_ticket", columnList = "ticket_id"),
        @Index(name = "idx_message_sender", columnList = "sent_by_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sent_by_id", nullable = false)
    private User sentBy;

    private String message;
    private String messageType; // TEXT, SYSTEM, NOTE

    @Builder.Default
    private boolean isInternal = false; // Internal note not visible to customer

    private String attachmentUrl;
    private String attachmentFileName;

    private Long attachmentSize; // In bytes

    @Builder.Default
    private boolean isResolution = false; // This message is the resolution
}
