package com.study.philstargram.feed.application;

import com.study.philstargram.feed.domain.FeedEntry;
import java.time.LocalDateTime;

public record FeedItem(Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {

    public static FeedItem from(FeedEntry entry) {
        return new FeedItem(entry.getPostId(), entry.getAuthorId(), entry.getAuthorNickname(), entry.getContentPreview(), entry.getCreatedAt());
    }
}
