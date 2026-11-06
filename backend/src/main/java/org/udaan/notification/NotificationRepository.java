package org.udaan.notification;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("select n from Notification n where n.userId = :userId order by n.createdAt desc")
    List<Notification> latest(UUID userId, Pageable page);

    @Query("select count(n) from Notification n where n.userId = :userId and n.readAt is null")
    long unreadCount(UUID userId);

    @Modifying
    @Query("update Notification n set n.readAt = CURRENT_TIMESTAMP where n.userId = :userId and n.readAt is null")
    int markAllRead(UUID userId);

    @Modifying
    @Query("update Notification n set n.readAt = CURRENT_TIMESTAMP where n.userId = :userId and n.readAt is null and n.id in :ids")
    int markRead(UUID userId, Collection<UUID> ids);
}
