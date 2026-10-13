package com.blogdock.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 최신순. 커서는 마지막으로 본 알림 id. */
    @Query("""
            select n from Notification n
            where n.recipientId = :recipientId and (:cursor is null or n.id < :cursor)
            order by n.id desc""")
    List<Notification> findPage(Long recipientId, Long cursor, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    /** 공감 알림은 같은 사람·같은 글이면 하루에 한 번만. */
    boolean existsByRecipientIdAndActorIdAndPostIdAndTypeAndCreatedAtAfter(
            Long recipientId, Long actorId, Long postId, String type, Instant since);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipientId = :recipientId and n.readAt is null")
    int readAll(Long recipientId, Instant now);

    @Modifying
    @Query("delete from Notification n where n.createdAt < :before")
    int deleteOlderThan(Instant before);
}
