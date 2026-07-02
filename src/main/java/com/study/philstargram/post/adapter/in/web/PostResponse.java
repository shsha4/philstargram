package com.study.philstargram.post.adapter.in.web;

import com.study.philstargram.post.application.PostResult;
import java.time.LocalDateTime;

public record PostResponse(Long id, Long authorId, String content, LocalDateTime createdAt) {

    public static PostResponse from(PostResult result) {
        return new PostResponse(result.id(), result.authorId(), result.content(), result.createdAt());
    }
}
