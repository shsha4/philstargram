package com.study.philstargram.post.application;

import com.study.philstargram.post.domain.PostWritten;
import java.time.LocalDateTime;

/**
 * 게시글이 생성될 때 발행되는 모듈 간 계약(integration event). post 는 누가 이 이벤트에
 * 반응하는지 알지 못한다. feed 는 이 이벤트로 게시글을 팔로워들의 피드에 팬아웃하고(쓰기 시점),
 * notification 은 팔로워들에게 알림을 생성하며(phase 2), search 는 게시글을 색인할 것이다(phase 6).
 *
 * <p>애그리거트가 발생시킨 도메인 이벤트 {@link PostWritten} 을 UseCase 가 이 타입으로 번역해
 * 발행한다. 도메인 이벤트는 모듈 내부 표현, 이 이벤트는 모듈 경계의 계약이다.
 */
public record PostCreatedEvent(Long postId, Long authorId, String contentPreview, LocalDateTime createdAt) {

    private static final int PREVIEW_LENGTH = 100;

    public static PostCreatedEvent from(PostWritten event) {
        String content = event.content();
        String preview = content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) : content;
        return new PostCreatedEvent(event.postId().value(), event.authorId(), preview, event.createdAt());
    }
}
