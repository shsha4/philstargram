package com.study.philstargram.notification.application;

import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 새 게시글이 작성되면 작성자를 팔로우하는 모든 사용자에게 알림을 생성한다. 이벤트 수신
 * (인바운드 어댑터)과 분리된 순수 유즈케이스다.
 */
@Service
public class NotifyNewPostUseCase {

    private final FollowQueryService followQueryService;
    private final MemberQueryService memberQueryService;
    private final NotificationRepository notificationRepository;

    public NotifyNewPostUseCase(FollowQueryService followQueryService, MemberQueryService memberQueryService, NotificationRepository notificationRepository) {
        this.followQueryService = followQueryService;
        this.memberQueryService = memberQueryService;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void execute(NotifyNewPostCommand command) {
        String authorNickname = memberQueryService.getSummary(command.authorId()).nickname();
        String message = authorNickname + "님이 새 게시글을 작성했습니다.";
        for (Long followerId : followQueryService.getFollowerIds(command.authorId())) {
            notificationRepository.save(Notification.create(followerId, NotificationType.NEW_POST, message, command.createdAt()));
        }
    }
}
