package com.study.philstargram.post.application;

import com.study.philstargram.post.domain.PostWritten;
import java.time.LocalDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * 게시글이 생성될 때 발행되는 모듈 간 계약(integration event). post 는 누가 이 이벤트에
 * 반응하는지 알지 못한다. feed 는 이 이벤트로 게시글을 팔로워들의 피드에 팬아웃하고(쓰기 시점),
 * notification 은 팔로워들에게 알림을 생성하며(phase 2), search 는 게시글을 색인할 것이다(phase 6).
 *
 * <p>애그리거트가 발생시킨 도메인 이벤트 {@link PostWritten} 을 UseCase 가 이 타입으로 번역해
 * 발행한다. 도메인 이벤트는 모듈 내부 표현, 이 이벤트는 모듈 경계의 계약이다.
 *
 * <p><b>event-carried state (phase 4):</b> 작성자 {@code authorNickname} 을 페이로드에 실어
 * 보낸다. 덕분에 feed/notification 컨슈머가 피드/알림 문구를 만들 때 member 를 동기 호출하지
 * 않는다 — 작성 시점에 post 가 한 번만 조회해 이벤트에 담고, N 개의 컨슈머가 재조회 없이 쓴다.
 *
 * <p><b>externalization (phase 4):</b> {@link Externalized} 로 이 이벤트를 Kafka 토픽
 * {@code post.created} 로 내보낸다. 파티션 키는 작성자 id 라서 한 작성자의 게시글은 같은
 * 파티션으로 가 순서가 보장된다. 이벤트 발행 레지스트리(Outbox)가 브로커 전송을 보증한다.
 */
@Externalized("post.created::#{#this.authorId().toString()}")
public record PostCreatedEvent(Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {

    private static final int PREVIEW_LENGTH = 100;

    public static PostCreatedEvent from(PostWritten event, String authorNickname) {
        String content = event.content();
        String preview = content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) : content;
        return new PostCreatedEvent(event.postId().value(), event.authorId(), authorNickname, preview, event.createdAt());
    }
}
