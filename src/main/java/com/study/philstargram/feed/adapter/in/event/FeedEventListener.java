package com.study.philstargram.feed.adapter.in.event;

import com.study.philstargram.feed.application.FanOutFeedCommand;
import com.study.philstargram.feed.application.FanOutFeedUseCase;
import com.study.philstargram.post.application.PostCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * feed 모듈의 이벤트 인바운드 어댑터. {@code post} 의 {@link PostCreatedEvent} 를 받아
 * {@link FanOutFeedUseCase} 에 위임한다(이벤트 → command 변환만 담당). web 컨트롤러와 동일한
 * 인입 어댑터 역할이다.
 *
 * <p>{@link ApplicationModuleListener} 는 발행 트랜잭션 커밋 후 별도 트랜잭션에서 비동기로
 * 실행되고, Outbox(이벤트 발행 레지스트리)로 발행이 영속화되어 재시작 시 미완료 건이
 * 재발행된다.
 */
@Component
class FeedEventListener {

    private final FanOutFeedUseCase fanOutFeedUseCase;

    FeedEventListener(FanOutFeedUseCase fanOutFeedUseCase) {
        this.fanOutFeedUseCase = fanOutFeedUseCase;
    }

    @ApplicationModuleListener
    void on(PostCreatedEvent event) {
        fanOutFeedUseCase.execute(new FanOutFeedCommand(
                event.postId(), event.authorId(), event.contentPreview(), event.createdAt()));
    }
}
