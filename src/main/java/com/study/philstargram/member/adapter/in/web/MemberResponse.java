package com.study.philstargram.member.adapter.in.web;

import com.study.philstargram.member.application.MemberResult;
import java.time.LocalDateTime;

public record MemberResponse(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {

    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(result.id(), result.email(), result.nickname(), result.bio(), result.createdAt());
    }
}
