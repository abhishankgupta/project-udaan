package org.udaan.notification;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationService {

    private final NotificationRepository notifications;

    NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    record Message(String type, String title, String body, String link, UUID actorId, UUID problemId) {
    }

    /** Notifies each recipient once, never the person who caused the event. */
    @Transactional
    void send(Collection<UUID> recipients, Message m) {
        Set<UUID> unique = new LinkedHashSet<>(recipients);
        unique.remove(null);
        if (m.actorId() != null) {
            unique.remove(m.actorId());
        }
        notifications.saveAll(unique.stream()
                .map(userId -> new Notification(userId, m.type(), m.title(), m.body(), m.link(), m.actorId(), m.problemId()))
                .toList());
    }

    record View(UUID id, String type, String title, String body, String link, boolean read, java.time.Instant createdAt) {
    }

    record Inbox(List<View> items, long unreadCount) {
    }

    @Transactional(readOnly = true)
    Inbox inbox(UUID userId, int size) {
        List<View> items = notifications.latest(userId, PageRequest.of(0, Math.min(Math.max(1, size), 100))).stream()
                .map(n -> new View(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(), n.getReadAt() != null, n.getCreatedAt()))
                .toList();
        return new Inbox(items, notifications.unreadCount(userId));
    }

    @Transactional(readOnly = true)
    long unreadCount(UUID userId) {
        return notifications.unreadCount(userId);
    }

    @Transactional
    long markRead(UUID userId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            notifications.markAllRead(userId);
        } else {
            notifications.markRead(userId, ids);
        }
        return notifications.unreadCount(userId);
    }
}
