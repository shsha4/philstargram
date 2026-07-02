package com.study.philstargram.notification.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Notification {

    private final Long id;
    private final Long recipientMemberId;
    private final NotificationType type;
    private final String message;
    private final LocalDateTime createdAt;

    private Notification(Long id, Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt) {
        this.id = id;
        this.recipientMemberId = recipientMemberId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static Notification create(Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt) {
        Objects.requireNonNull(recipientMemberId, "recipientMemberId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
        return new Notification(null, recipientMemberId, type, message, createdAt);
    }

    public static Notification reconstitute(Long id, Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt) {
        return new Notification(id, recipientMemberId, type, message, createdAt);
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
}
