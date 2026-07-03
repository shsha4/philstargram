package com.study.philstargram.post.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 콘텐츠 길이 제한 같은 비즈니스 규칙은 도메인({@code Post.write})이 단일 소스로 소유한다.
 * 여기서는 요청의 형태(작성자 존재, 본문 공백 아님)만 최소로 검증해 조기 실패시킨다.
 */
public record CreatePostRequest(
        @NotNull Long authorId,
        @NotBlank String content
) {
}
