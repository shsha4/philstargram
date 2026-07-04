package com.study.philstargram.post.application;

import com.study.philstargram.post.domain.Post;
import java.time.LocalDateTime;

/**
 * 다른 모듈(feed 의 읽기 시점 pull)에 노출하는 게시글 요약(phase 5c). 도메인 타입을 노출하지 않고,
 * feed 가 그대로 피드에 병합할 수 있도록 닉네임 스냅샷과 본문 미리보기를 담는다.
 */
public record RecentPostSummary(Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {

    private static final int PREVIEW_LENGTH = 100;

    public static RecentPostSummary from(Post post) {
        String content = post.getContent();
        String preview = content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) : content;
        return new RecentPostSummary(post.getId().value(), post.getAuthorId(), post.getAuthorNickname(), preview, post.getCreatedAt());
    }
}
