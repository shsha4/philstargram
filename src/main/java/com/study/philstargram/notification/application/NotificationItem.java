package com.study.philstargram.notification.application;

import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationType;
import java.time.LocalDateTime;

public record NotificationItem(Long id, NotificationType type, String message, LocalDateTime createdAt) {

    public static NotificationItem from(Notification notification) {
        return new NotificationItem(notification.getId(), notification.getType(), notification.getMessage(), notification.getCreatedAt());
    }
}
