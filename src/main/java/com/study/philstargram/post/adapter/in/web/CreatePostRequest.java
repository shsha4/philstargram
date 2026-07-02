package com.study.philstargram.post.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull Long authorId,
        @NotBlank @Size(max = 2000) String content
) {
}
