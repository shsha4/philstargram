package com.study.philstargram.member.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 이메일 값 객체. 형식 불변식을 도메인이 스스로 보장한다(더 이상 web DTO 의 {@code @Email} 에
 * 위임하지 않는다). 영속성/Result/Summary 경계에서는 {@link #value()} 로 String 을 꺼내 쓴다.
 */
public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "email must not be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
