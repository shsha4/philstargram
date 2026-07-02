package com.study.philstargram.notification.application;

import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import com.study.philstargram.post.application.PostCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class NotifyFollowersOnPostCreated {

    private final FollowQueryService followQueryService;
    private final MemberQueryService memberQueryService;
    private final NotificationRepository notificationRepository;

    NotifyFollowersOnPostCreated(FollowQueryService followQueryService, MemberQueryService memberQueryService, NotificationRepository notificationRepository) {
        this.followQueryService = followQueryService;
        this.memberQueryService = memberQueryService;
        this.notificationRepository = notificationRepository;
    }

    @ApplicationModuleListener
    void on(PostCreatedEvent event) {
        String authorNickname = memberQueryService.getSummary(event.authorId()).nickname();
        String message = authorNickname + "님이 새 게시글을 작성했습니다.";
        for (Long followerId : followQueryService.getFollowerIds(event.authorId())) {
            notificationRepository.save(Notification.create(followerId, NotificationType.NEW_POST, message, event.createdAt()));
        }
    }
}
