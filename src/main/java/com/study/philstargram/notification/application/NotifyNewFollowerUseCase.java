package com.study.philstargram.notification.application;

import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 누군가 나를 팔로우하면 팔로위(followee)에게 알림을 생성한다. 이벤트 수신(인바운드
 * 어댑터)과 분리된 순수 유즈케이스다.
 */
@Service
public class NotifyNewFollowerUseCase {

    private final MemberQueryService memberQueryService;
    private final NotificationRepository notificationRepository;

    public NotifyNewFollowerUseCase(MemberQueryService memberQueryService, NotificationRepository notificationRepository) {
        this.memberQueryService = memberQueryService;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void execute(NotifyNewFollowerCommand command) {
        String followerNickname = memberQueryService.getSummary(command.followerId()).nickname();
        String message = followerNickname + "님이 회원님을 팔로우했습니다.";
        notificationRepository.save(Notification.create(command.followeeId(), NotificationType.NEW_FOLLOWER, message, command.followedAt()));
    }
}
