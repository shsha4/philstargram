package com.study.philstargram.notification.adapter.out.persistence;

import com.study.philstargram.notification.domain.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_member_id", nullable = false)
    private Long recipientMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "dedup_key", nullable = false, length = 100)
    private String dedupKey;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(Long id, Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt, String dedupKey) {
        this.id = id;
        this.recipientMemberId = recipientMemberId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.dedupKey = dedupKey;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientMemberId() {
        return recipientMemberId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDedupKey() {
        return dedupKey;
    }
}
