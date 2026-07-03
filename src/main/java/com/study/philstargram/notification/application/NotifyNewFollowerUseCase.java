package com.study.philstargram.notification.application;

import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 누군가 나를 팔로우하면 팔로위(followee)에게 알림을 생성한다. 이벤트 수신(인바운드
 * 어댑터)과 분리된 순수 유즈케이스다.
 *
 * <p><b>phase 4 결합 제거:</b> 팔로워 닉네임을 이벤트가 실어오므로(event-carried state) member
 * 동기 조회를 없앴다. 이 유즈케이스는 이제 자기 모듈(notification)만 의존한다.
 */
@Service
public class NotifyNewFollowerUseCase {

    private final NotificationRepository notificationRepository;

    public NotifyNewFollowerUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void execute(NotifyNewFollowerCommand command) {
        String message = command.followerNickname() + "님이 회원님을 팔로우했습니다.";
        notificationRepository.save(Notification.create(command.followeeId(), NotificationType.NEW_FOLLOWER, message, command.followedAt()));
    }
}
