package com.study.philstargram.member.application;

import com.study.philstargram.member.domain.Member;
import java.time.LocalDateTime;

public record MemberResult(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {

    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId().value(),
                member.getEmail().value(),
                member.getNickname().value(),
                member.getBio(),
                member.getCreatedAt());
    }
}
