package com.study.philstargram.member.adapter.in.web;

import jakarta.validation.constraints.Size;

/**
 * email/nickname 의 형식·길이 규칙은 도메인 값 객체({@code Email}/{@code Nickname})가 스스로
 * 보장하므로 web 계층에 중복 검증을 두지 않는다(위반 시 도메인이 던진
 * {@code IllegalArgumentException} 을 GlobalExceptionHandler 가 400 으로 변환). bio 는 도메인
 * 불변식이 없는 선택 필드라 web 에서만 최대 길이를 제한한다.
 */
public record SignUpMemberRequest(
        String email,
        String nickname,
        @Size(max = 500) String bio
) {
}
