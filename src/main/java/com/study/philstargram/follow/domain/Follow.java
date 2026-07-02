package com.study.philstargram.follow.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Follow {

    private final Long id;
    private final Long followerId;
    private final Long followeeId;
    private final LocalDateTime followedAt;

    private Follow(Long id, Long followerId, Long followeeId, LocalDateTime followedAt) {
        this.id = id;
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.followedAt = followedAt;
    }

    public static Follow create(Long followerId, Long followeeId) {
        Objects.requireNonNull(followerId, "followerId must not be null");
        Objects.requireNonNull(followeeId, "followeeId must not be null");
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }
        return new Follow(null, followerId, followeeId, LocalDateTime.now());
    }

    public static Follow reconstitute(Long id, Long followerId, Long followeeId, LocalDateTime followedAt) {
        return new Follow(id, followerId, followeeId, followedAt);
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
