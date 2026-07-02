package com.study.philstargram.notification.application;

import com.study.philstargram.notification.domain.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMyNotificationsUseCase {

    private static final int DEFAULT_SIZE = 20;

    private final NotificationRepository notificationRepository;

    public GetMyNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> execute(Long memberId) {
        return notificationRepository.findRecentByRecipientMemberId(memberId, DEFAULT_SIZE).stream()
                .map(NotificationItem::from)
                .toList();
    }
}
