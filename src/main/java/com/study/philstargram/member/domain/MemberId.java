package com.study.philstargram.member.domain;

import java.util.Objects;

/**
 * member 모듈의 내부 식별자 타입. 모듈 경계(다른 모듈이 받는 authorId/followerId 등)에서는
 * 결합을 피하려 raw {@code Long} 을 쓰지만, 모듈 내부에서는 타입드 ID 로 식별자 혼동을 막는다.
 */
public record MemberId(Long value) {

    public MemberId {
        Objects.requireNonNull(value, "memberId must not be null");
    }

    public static MemberId of(Long value) {
        return new MemberId(value);
    }
}
