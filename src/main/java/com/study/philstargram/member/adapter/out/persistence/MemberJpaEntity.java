package com.study.philstargram.member.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private String bio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MemberJpaEntity() {
    }

    public MemberJpaEntity(Long id, String email, String nickname, String bio, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.bio = bio;
        this.createdAt = createdAt;
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
