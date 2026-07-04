package com.study.philstargram.notification.application;

import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 새 게시글이 작성되면 작성자를 팔로우하는 모든 사용자에게 알림을 생성한다. 이벤트 수신
 * (인바운드 어댑터)과 분리된 순수 유즈케이스다.
 *
 * <p><b>phase 4 결합 제거:</b> 작성자 닉네임을 이벤트가 실어오므로(event-carried state) member
 * 동기 조회를 없앴다. notification 은 이제 follow 만 호출한다.
 */
@Service
public class NotifyNewPostUseCase {

    private final FollowQueryService followQueryService;
    private final NotificationRepository notificationRepository;

    public NotifyNewPostUseCase(FollowQueryService followQueryService, NotificationRepository notificationRepository) {
        this.followQueryService = followQueryService;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void execute(NotifyNewPostCommand command) {
        String message = command.authorNickname() + "님이 새 게시글을 작성했습니다.";
        for (Long followerId : followQueryService.getFollowerIds(command.authorId())) {
            // dedupKey = 수신자 + 게시글: 같은 게시글 알림이 같은 사람에게 두 번 만들어지지 않게 한다(phase 5b).
            String dedupKey = "NEW_POST:" + followerId + ":" + command.postId();
            notificationRepository.save(Notification.create(followerId, NotificationType.NEW_POST, message, command.createdAt(), dedupKey));
        }
    }
}
