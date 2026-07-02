package com.study.philstargram.notification.domain;

import java.util.List;

public interface NotificationRepository {

    Notification save(Notification notification);

    List<Notification> findRecentByRecipientMemberId(Long recipientMemberId, int limit);
}
