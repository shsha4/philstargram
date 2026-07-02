package com.study.philstargram.member.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Member {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String bio;
    private final LocalDateTime createdAt;

    private Member(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    public static Member signUp(String email, String nickname, String bio) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(nickname, "nickname must not be null");
        return new Member(null, email, nickname, bio, LocalDateTime.now());
    }

    public static Member reconstitute(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {
        return new Member(id, email, nickname, bio, createdAt);
    }

    public Member withId(Long id) {
        return new Member(id, this.email, this.nickname, this.bio, this.createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getBio() {
        return bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
