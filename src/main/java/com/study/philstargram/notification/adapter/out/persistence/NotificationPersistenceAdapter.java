package com.study.philstargram.notification.adapter.out.persistence;

import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    NotificationPersistenceAdapter(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        return toDomain(notificationJpaRepository.save(toEntity(notification)));
    }

    @Override
    public List<Notification> findRecentByRecipientMemberId(Long recipientMemberId, int limit) {
        return notificationJpaRepository.findByRecipientMemberIdOrderByCreatedAtDesc(recipientMemberId, PageRequest.of(0, limit)).stream()
                .map(NotificationPersistenceAdapter::toDomain)
                .toList();
    }

    private static NotificationJpaEntity toEntity(Notification notification) {
        return new NotificationJpaEntity(notification.getId(), notification.getRecipientMemberId(), notification.getType(), notification.getMessage(), notification.getCreatedAt());
    }

    private static Notification toDomain(NotificationJpaEntity entity) {
        return Notification.reconstitute(entity.getId(), entity.getRecipientMemberId(), entity.getType(), entity.getMessage(), entity.getCreatedAt());
    }
}
