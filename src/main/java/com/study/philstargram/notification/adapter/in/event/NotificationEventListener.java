package com.study.philstargram.notification.adapter.in.event;

import com.study.philstargram.follow.application.MemberFollowedEvent;
import com.study.philstargram.notification.application.NotifyNewFollowerCommand;
import com.study.philstargram.notification.application.NotifyNewFollowerUseCase;
import com.study.philstargram.notification.application.NotifyNewPostCommand;
import com.study.philstargram.notification.application.NotifyNewPostUseCase;
import com.study.philstargram.post.application.PostCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * notification 모듈의 이벤트 인바운드 어댑터. 다른 모듈이 발행한 이벤트를 받아 각 유즈케이스로
 * 위임한다(이벤트 → command 변환만 담당). 각 리스너는 발행 트랜잭션 커밋 후 별도 트랜잭션에서
 * 비동기 실행되며 Outbox 로 발행이 영속화된다.
 */
@Component
class NotificationEventListener {

    private final NotifyNewPostUseCase notifyNewPostUseCase;
    private final NotifyNewFollowerUseCase notifyNewFollowerUseCase;

    NotificationEventListener(NotifyNewPostUseCase notifyNewPostUseCase, NotifyNewFollowerUseCase notifyNewFollowerUseCase) {
        this.notifyNewPostUseCase = notifyNewPostUseCase;
        this.notifyNewFollowerUseCase = notifyNewFollowerUseCase;
    }

    @ApplicationModuleListener
    void on(PostCreatedEvent event) {
        notifyNewPostUseCase.execute(new NotifyNewPostCommand(event.authorId(), event.createdAt()));
    }

    @ApplicationModuleListener
    void on(MemberFollowedEvent event) {
        notifyNewFollowerUseCase.execute(new NotifyNewFollowerCommand(event.followerId(), event.followeeId(), event.followedAt()));
    }
}
