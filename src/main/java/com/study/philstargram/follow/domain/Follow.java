package com.study.philstargram.follow.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Follow {

    private FollowId id;                 // 저장 시 DB 가 생성한 식별자를 부여받으므로 final 이 아니다
    private final Long followerId;       // member 모듈 식별자 → 모듈 결합을 피하려 raw Long 유지
    private final Long followeeId;       // member 모듈 식별자 → 모듈 결합을 피하려 raw Long 유지
    private final LocalDateTime followedAt;
    private boolean created;             // create() 로 갓 생성된 신규 팔로우인지(도메인 이벤트 발생 대상)

    private Follow(FollowId id, Long followerId, Long followeeId, LocalDateTime followedAt) {
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
        Follow follow = new Follow(null, followerId, followeeId, LocalDateTime.now());
        follow.created = true;
        return follow;
    }

    public static Follow reconstitute(Long id, Long followerId, Long followeeId, LocalDateTime followedAt) {
        return new Follow(FollowId.of(id), followerId, followeeId, followedAt);
    }

    /** 저장소가 생성한 식별자를 신규 애그리거트에 1회 부여한다. */
    public void assignId(FollowId id) {
        if (this.id != null) {
            throw new IllegalStateException("이미 식별자가 부여된 팔로우입니다.");
        }
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    /**
     * 애그리거트가 누적한 도메인 이벤트를 꺼내 비운다. UseCase/adapter 가 이를 드레인해
     * 모듈 간 이벤트로 번역·발행한다.
     */
    public List<MemberFollowed> pullDomainEvents() {
        if (!created) {
            return List.of();
        }
        created = false;
        return List.of(new MemberFollowed(followerId, followeeId, followedAt));
    }

    public FollowId getId() {
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
