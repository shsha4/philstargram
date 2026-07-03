package com.study.philstargram.member.domain;

import java.util.Objects;

/**
 * 닉네임 값 객체. 공백 불가 + 길이 2~50자 불변식을 도메인이 스스로 보장한다.
 * 영속성/Result/Summary 경계에서는 {@link #value()} 로 String 을 꺼내 쓴다.
 */
public record Nickname(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;

    public Nickname {
        Objects.requireNonNull(value, "nickname must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 " + MIN_LENGTH + "~" + MAX_LENGTH + "자여야 합니다.");
        }
    }

    public static Nickname of(String value) {
        return new Nickname(value);
    }
}
