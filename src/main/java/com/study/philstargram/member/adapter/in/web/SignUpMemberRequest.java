package com.study.philstargram.member.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpMemberRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 50) String nickname,
        @Size(max = 500) String bio
) {
}
