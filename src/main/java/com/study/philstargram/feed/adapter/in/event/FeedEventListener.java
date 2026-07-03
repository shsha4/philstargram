package com.study.philstargram.feed.adapter.in.event;

import com.study.philstargram.feed.application.FanOutFeedCommand;
import com.study.philstargram.feed.application.FanOutFeedUseCase;
import com.study.philstargram.post.application.PostCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * feed 모듈의 이벤트 인바운드 어댑터. Kafka 토픽 {@code post.created} 를 구독해
 * {@link PostCreatedEvent} 로 받아 {@link FanOutFeedUseCase} 에 위임한다(메시지 → command 변환만
 * 담당). web 컨트롤러가 UseCase 를 호출하는 것과 동일한 인입 어댑터다.
 *
 * <p>phase 3 까지는 인프로세스 {@code @ApplicationModuleListener} 였으나, phase 4 에서 전송을
 * Kafka 로 바꿔 컨슈머를 독립 배포 가능하게 만들었다. feed 전용 컨슈머 그룹({@code feed})을 써
 * notification 컨슈머와 별개로 같은 이벤트를 각자 수신한다.
 *
 * <p>JSON 페이로드는 Spring Kafka 의 메시지 컨버터(Jackson)가 리스너 파라미터 타입을 보고
 * 역직렬화한다. 계약은 "토픽 + JSON 형태"이며, 생산자 타입 헤더에 의존하지 않는다(모놀리스라
 * 지금은 생산자 이벤트 레코드를 그대로 재사용하지만, 실제 분리 시 컨슈머가 자체 레코드를 두면 된다).
 *
 * <p><b>전달 보장:</b> at-least-once. 재전달 시 팬아웃이 중복될 수 있으나, 컨슈머 idempotency 는
 * phase 5(Redis/일관성)에서 다룬다(현재는 트레이드오프로 남겨둔 상태).
 */
@Component
class FeedEventListener {

    private final FanOutFeedUseCase fanOutFeedUseCase;

    FeedEventListener(FanOutFeedUseCase fanOutFeedUseCase) {
        this.fanOutFeedUseCase = fanOutFeedUseCase;
    }

    @KafkaListener(topics = "post.created", groupId = "feed")
    void onPostCreated(PostCreatedEvent event) {
        fanOutFeedUseCase.execute(new FanOutFeedCommand(
                event.postId(), event.authorId(), event.authorNickname(), event.contentPreview(), event.createdAt()));
    }
}
