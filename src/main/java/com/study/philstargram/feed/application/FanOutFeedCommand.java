package com.study.philstargram.feed.application;

import java.time.LocalDateTime;

public record FanOutFeedCommand(Long postId, Long authorId, String contentPreview, LocalDateTime createdAt) {
}
