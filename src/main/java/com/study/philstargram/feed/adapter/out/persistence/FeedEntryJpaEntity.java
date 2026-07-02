package com.study.philstargram.feed.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_entries")
public class FeedEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_member_id", nullable = false)
    private Long ownerMemberId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_nickname", nullable = false)
    private String authorNickname;

    @Column(name = "content_preview", nullable = false, length = 200)
    private String contentPreview;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FeedEntryJpaEntity() {
    }

    public FeedEntryJpaEntity(Long id, Long ownerMemberId, Long postId, Long authorId, String authorNickname, String contentPreview, LocalDateTime createdAt) {
        this.id = id;
        this.ownerMemberId = ownerMemberId;
        this.postId = postId;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.contentPreview = contentPreview;
        this.createdAt = createdAt;
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
