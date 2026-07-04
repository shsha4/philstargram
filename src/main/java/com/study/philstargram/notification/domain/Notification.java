package com.study.philstargram.notification.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Notification {

    private final Long id;
    private final Long recipientMemberId;
    private final NotificationType type;
    private final String message;
    private final LocalDateTime createdAt;
    /**
     * 멱등 키(phase 5b). 소스 이벤트로부터 만든 자연 식별자({@code type:recipient:sourceId})로,
     * at-least-once 재전달 시 같은 알림이 중복 생성되지 않도록 유니크 제약의 근거가 된다.
     */
    private final String dedupKey;

    private Notification(Long id, Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt, String dedupKey) {
        this.id = id;
        this.recipientMemberId = recipientMemberId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.dedupKey = dedupKey;
    }

    public static Notification create(Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt, String dedupKey) {
        Objects.requireNonNull(recipientMemberId, "recipientMemberId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(dedupKey, "dedupKey must not be null");
        return new Notification(null, recipientMemberId, type, message, createdAt, dedupKey);
    }

    public static Notification reconstitute(Long id, Long recipientMemberId, NotificationType type, String message, LocalDateTime createdAt, String dedupKey) {
        return new Notification(id, recipientMemberId, type, message, createdAt, dedupKey);
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
