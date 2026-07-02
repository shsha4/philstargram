package com.study.philstargram.notification.adapter.in.web;

import com.study.philstargram.notification.application.NotificationItem;
import com.study.philstargram.notification.domain.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationType type, String message, LocalDateTime createdAt) {

    public static NotificationResponse from(NotificationItem item) {
        return new NotificationResponse(item.id(), item.type(), item.message(), item.createdAt());
    }
}
