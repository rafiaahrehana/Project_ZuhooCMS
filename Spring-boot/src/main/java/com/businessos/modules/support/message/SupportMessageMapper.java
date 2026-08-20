package com.businessos.modules.support.message;

public class SupportMessageMapper {
    public static SupportMessageResponse toResponse(SupportMessage entity) {
        if (entity == null) {
            return null;
        }

        return SupportMessageResponse.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .sentById(entity.getSentBy() != null ? entity.getSentBy().getId() : null)
                .sentByName(entity.getSentBy() != null ? entity.getSentBy().getFullName() : null)
                .message(entity.getMessage())
                .messageType(entity.getMessageType())
                .isInternal(entity.isInternal())
                .attachmentUrl(entity.getAttachmentUrl())
                .attachmentFileName(entity.getAttachmentFileName())
                .attachmentSize(entity.getAttachmentSize())
                .isResolution(entity.isResolution())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
