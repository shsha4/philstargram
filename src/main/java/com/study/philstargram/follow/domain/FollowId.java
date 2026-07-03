package com.study.philstargram.follow.domain;

import java.util.Objects;

/**
 * follow 모듈의 내부 식별자 타입. 모듈 경계에서는 raw {@code Long} 을 쓰지만, 모듈 내부에서는
 * 타입드 ID 로 식별자 혼동을 막는다.
 */
public record FollowId(Long value) {

    public FollowId {
        Objects.requireNonNull(value, "followId must not be null");
    }

    public static FollowId of(Long value) {
        return new FollowId(value);
    }
}
