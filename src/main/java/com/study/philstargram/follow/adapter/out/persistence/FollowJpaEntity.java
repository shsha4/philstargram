package com.study.philstargram.follow.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows")
public class FollowJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    @Column(name = "followed_at", nullable = false)
    private LocalDateTime followedAt;

    protected FollowJpaEntity() {
    }

    public FollowJpaEntity(Long id, Long followerId, Long followeeId, LocalDateTime followedAt) {
        this.id = id;
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.followedAt = followedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFolloweeId() {
        return followeeId;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }
}
