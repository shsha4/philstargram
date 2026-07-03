package com.study.philstargram.post.application;

import com.study.philstargram.post.domain.Post;
import java.time.LocalDateTime;

public record PostResult(Long id, Long authorId, String content, LocalDateTime createdAt) {

    public static PostResult from(Post post) {
        return new PostResult(post.getId().value(), post.getAuthorId(), post.getContent(), post.getCreatedAt());
    }
}
