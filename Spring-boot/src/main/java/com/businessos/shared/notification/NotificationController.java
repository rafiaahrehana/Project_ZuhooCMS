package com.businessos.shared.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(unreadOnly,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok("All notifications marked as read");
    }
}
