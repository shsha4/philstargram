package com.study.philstargram.notification.adapter.in.event;

import com.study.philstargram.follow.application.MemberFollowedEvent;
import com.study.philstargram.notification.application.NotifyNewFollowerCommand;
import com.study.philstargram.notification.application.NotifyNewFollowerUseCase;
import com.study.philstargram.notification.application.NotifyNewPostCommand;
import com.study.philstargram.notification.application.NotifyNewPostUseCase;
import com.study.philstargram.post.application.PostCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * notification 모듈의 이벤트 인바운드 어댑터. Kafka 토픽 {@code post.created} 와
 * {@code member.followed} 를 구독해 각 이벤트로 받아 유즈케이스로 위임한다(메시지 → command
 * 변환만 담당). JSON 페이로드는 Spring Kafka 메시지 컨버터(Jackson)가 파라미터 타입으로 역직렬화한다.
 *
 * <p>phase 4 에서 인프로세스 {@code @ApplicationModuleListener} → Kafka 컨슈머로 전환했다.
 * notification 전용 컨슈머 그룹({@code notification})을 써 feed 컨슈머와 독립적으로 같은
 * {@code post.created} 이벤트를 각자 수신한다.
 *
 * <p><b>전달 보장:</b> at-least-once — 컨슈머 idempotency 는 phase 5 에서 다룬다.
 */
@Component
class NotificationEventListener {

    private final NotifyNewPostUseCase notifyNewPostUseCase;
    private final NotifyNewFollowerUseCase notifyNewFollowerUseCase;

    NotificationEventListener(NotifyNewPostUseCase notifyNewPostUseCase, NotifyNewFollowerUseCase notifyNewFollowerUseCase) {
        this.notifyNewPostUseCase = notifyNewPostUseCase;
        this.notifyNewFollowerUseCase = notifyNewFollowerUseCase;
    }

    @KafkaListener(topics = "post.created", groupId = "notification")
    void onPostCreated(PostCreatedEvent event) {
        notifyNewPostUseCase.execute(new NotifyNewPostCommand(event.authorId(), event.authorNickname(), event.createdAt()));
    }

    @KafkaListener(topics = "member.followed", groupId = "notification")
    void onMemberFollowed(MemberFollowedEvent event) {
        notifyNewFollowerUseCase.execute(new NotifyNewFollowerCommand(
                event.followerId(), event.followeeId(), event.followerNickname(), event.followedAt()));
    }
}
