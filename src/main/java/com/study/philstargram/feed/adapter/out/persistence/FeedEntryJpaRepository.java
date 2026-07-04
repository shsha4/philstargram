package com.study.philstargram.feed.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FeedEntryJpaRepository extends JpaRepository<FeedEntryJpaEntity, Long> {

    List<FeedEntryJpaEntity> findByOwnerMemberIdOrderByCreatedAtDesc(Long ownerMemberId, Pageable pageable);

    /**
     * 멱등 삽입(phase 5b). 유니크키 {@code (owner_member_id, post_id)} 충돌 시 무시한다. JPA
     * {@code save()} 는 충돌 시 예외(→ 트랜잭션 abort)를 내므로, 팬아웃 루프에서 안전하도록
     * native {@code ON CONFLICT DO NOTHING} 을 쓴다.
     */
    @Modifying
    @Query(value = "INSERT INTO feed_entries "
            + "(owner_member_id, post_id, author_id, author_nickname, content_preview, created_at) "
            + "VALUES (:ownerMemberId, :postId, :authorId, :authorNickname, :contentPreview, :createdAt) "
            + "ON CONFLICT (owner_member_id, post_id) DO NOTHING", nativeQuery = true)
    void insertIgnoringDuplicate(@Param("ownerMemberId") Long ownerMemberId, @Param("postId") Long postId,
            @Param("authorId") Long authorId, @Param("authorNickname") String authorNickname,
            @Param("contentPreview") String contentPreview, @Param("createdAt") LocalDateTime createdAt);
}
