package com.study.philstargram.feed.domain;

import java.time.LocalDateTime;

/**
 * 사용자 타임라인의 한 행(row). 작성자의 게시글이 생성될 때 쓰기 시점 팬아웃으로 미리
 * 저장된다. 비정규화된 스냅샷(authorNickname, contentPreview)을 함께 담아, 피드 조회 시
 * member/post 를 다시 호출할 필요가 없도록 한다.
 */
public class FeedEntry {

    private final Long id;
    private final Long ownerMemberId;
    private final Long postId;
    private final Long authorId;
    private final String authorNickname;
    private final String contentPreview;
    private final LocalDateTime createdAt;

    private FeedEntry(Long id, Long ownerMemberId, Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {
        this.id = id;
        this.ownerMemberId = ownerMemberId;
        this.postId = postId;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.contentPreview = contentPreview;
        this.createdAt = createdAt;
    }

    public static FeedEntry create(Long ownerMemberId, Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {
        return new FeedEntry(null, ownerMemberId, postId, authorId, authorNickname, contentPreview, createdAt);
    }

    public static FeedEntry reconstitute(Long id, Long ownerMemberId, Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {
        return new FeedEntry(id, ownerMemberId, postId, authorId, authorNickname, contentPreview, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerMemberId() {
        return ownerMemberId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
