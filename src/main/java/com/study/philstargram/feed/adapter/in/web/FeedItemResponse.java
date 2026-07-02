package com.study.philstargram.feed.adapter.in.web;

import com.study.philstargram.feed.application.FeedItem;
import java.time.LocalDateTime;

public record FeedItemResponse(Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {

    public static FeedItemResponse from(FeedItem item) {
        return new FeedItemResponse(item.postId(), item.authorId(), item.authorNickname(), item.contentPreview(), item.createdAt());
    }
}
