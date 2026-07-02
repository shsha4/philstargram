package com.study.philstargram.notification.application;

import com.study.philstargram.follow.application.MemberFollowedEvent;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class NotifyFolloweeOnMemberFollowed {

    private final MemberQueryService memberQueryService;
    private final NotificationRepository notificationRepository;

    NotifyFolloweeOnMemberFollowed(MemberQueryService memberQueryService, NotificationRepository notificationRepository) {
        this.memberQueryService = memberQueryService;
        this.notificationRepository = notificationRepository;
    }

    @ApplicationModuleListener
    void on(MemberFollowedEvent event) {
        String followerNickname = memberQueryService.getSummary(event.followerId()).nickname();
        String message = followerNickname + "님이 회원님을 팔로우했습니다.";
        notificationRepository.save(Notification.create(event.followeeId(), NotificationType.NEW_FOLLOWER, message, event.followedAt()));
    }
}
