package com.study.philstargram.post.application;

public record CreatePostCommand(Long authorId, String content) {
}
