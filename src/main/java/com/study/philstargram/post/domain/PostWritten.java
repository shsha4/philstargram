package com.study.philstargram.post.domain;

import java.time.LocalDateTime;

/**
 * post 애그리거트가 발생시키는 도메인 이벤트. 모듈 내부 표현이며, UseCase 가 이를 모듈 간
 * 계약인 {@code post.application.PostCreatedEvent} 로 번역해 발행한다. 도메인을 framework-free 로
 * 유지하기 위해 Spring/JPA 에 의존하지 않는다.
 */
public record PostWritten(PostId postId, Long authorId, String content, LocalDateTime createdAt) {
}
