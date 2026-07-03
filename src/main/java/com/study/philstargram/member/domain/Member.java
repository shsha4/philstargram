package com.study.philstargram.member.domain;

import java.time.LocalDateTime;

public class Member {

    private final MemberId id;
    private final Email email;
    private final Nickname nickname;
    private final String bio;
    private final LocalDateTime createdAt;

    private Member(MemberId id, Email email, Nickname nickname, String bio, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    public static Member signUp(String email, String nickname, String bio) {
        return new Member(null, Email.of(email), Nickname.of(nickname), bio, LocalDateTime.now());
    }

    public static Member reconstitute(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {
        return new Member(MemberId.of(id), Email.of(email), Nickname.of(nickname), bio, createdAt);
    }

    public Member withId(Long id) {
        return new Member(MemberId.of(id), this.email, this.nickname, this.bio, this.createdAt);
    }

    public MemberId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public Nickname getNickname() {
        return nickname;
    }

    public String getBio() {
        return bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
