package com.study.philstargram.post.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Post {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final Long id;
    private final Long authorId;
    private final String content;
    private final LocalDateTime createdAt;

    private Post(Long id, Long authorId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Post write(Long authorId, String content) {
        Objects.requireNonNull(authorId, "authorId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("게시글 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("게시글 내용은 " + MAX_CONTENT_LENGTH + "자를 초과할 수 없습니다.");
        }
        return new Post(null, authorId, content, LocalDateTime.now());
    }

    public static Post reconstitute(Long id, Long authorId, String content, LocalDateTime createdAt) {
        return new Post(id, authorId, content, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
