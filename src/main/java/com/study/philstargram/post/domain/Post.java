package com.study.philstargram.post.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Post {

    /** 게시글 본문 길이 제한의 단일 소스. 다른 계층(web DTO 등)에서 중복 정의하지 않는다. */
    public static final int MAX_CONTENT_LENGTH = 2000;

    private PostId id;                 // 저장 시 DB 가 생성한 식별자를 부여받으므로 final 이 아니다
    private final Long authorId;       // member 모듈 식별자 → 모듈 결합을 피하려 raw Long 유지
    private final String content;
    private final LocalDateTime createdAt;
    private boolean written;           // write() 로 갓 생성된 신규 게시글인지(도메인 이벤트 발생 대상)

    private Post(PostId id, Long authorId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Post write(Long authorId, String content) {
        Objects.requireNonNull(authorId, "authorId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("게시글 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("게시글 내용은 " + MAX_CONTENT_LENGTH + "자를 초과할 수 없습니다.");
        }
        Post post = new Post(null, authorId, content, LocalDateTime.now());
        post.written = true;
        return post;
    }

    public static Post reconstitute(Long id, Long authorId, String content, LocalDateTime createdAt) {
        return new Post(PostId.of(id), authorId, content, createdAt);
    }

    /**
     * 저장소가 생성한 식별자를 신규 애그리거트에 1회 부여한다. DB 가 식별자를 생성하는 구조라
     * 식별자를 담은 도메인 이벤트는 이 호출 이후에야 만들어질 수 있다({@link #pullDomainEvents()}).
     */
    public void assignId(PostId id) {
        if (this.id != null) {
            throw new IllegalStateException("이미 식별자가 부여된 게시글입니다.");
        }
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    /**
     * 애그리거트가 누적한 도메인 이벤트를 꺼내 비운다. 식별자 부여 후 호출해야 postId 가 채워진다.
     * UseCase/adapter 가 이를 드레인해 모듈 간 이벤트로 번역·발행한다.
     */
    public List<PostWritten> pullDomainEvents() {
        if (!written) {
            return List.of();
        }
        written = false;
        return List.of(new PostWritten(id, authorId, content, createdAt));
    }

    public PostId getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
