package com.study.philstargram.member.application;

import com.study.philstargram.member.domain.Member;
import java.time.LocalDateTime;

public record MemberResult(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {

    public static MemberResult from(Member member) {
        return new MemberResult(member.getId(), member.getEmail(), member.getNickname(), member.getBio(), member.getCreatedAt());
    }
}
