package com.study.philstargram.notification.domain;

import java.util.List;

public interface NotificationRepository {

    /**
     * 알림을 저장한다. <b>멱등(phase 5b):</b> 같은 {@code dedupKey} 가 이미 있으면 무시한다
     * (ON CONFLICT DO NOTHING). at-least-once 재전달로 같은 알림이 중복 생성되지 않는다.
     */
    void save(Notification notification);

    List<Notification> findRecentByRecipientMemberId(Long recipientMemberId, int limit);
}
