package org.udaan.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NotificationController {

    private final NotificationService notifications;

    NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    record MarkReadRequest(List<UUID> ids) {
    }

    @GetMapping("/api/v1/notifications")
    NotificationService.Inbox inbox(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "30") int size) {
        return notifications.inbox(UUID.fromString(jwt.getSubject()), size);
    }

    @GetMapping("/api/v1/notifications/unread-count")
    Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("unreadCount", notifications.unreadCount(UUID.fromString(jwt.getSubject())));
    }

    /** Marks the given notifications read, or all of them when no ids are sent. */
    @PostMapping("/api/v1/notifications/read")
    Map<String, Long> markRead(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) MarkReadRequest request) {
        return Map.of("unreadCount", notifications.markRead(UUID.fromString(jwt.getSubject()), request == null ? null : request.ids()));
    }
}
