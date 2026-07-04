package com.study.philstargram.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    List<NotificationJpaEntity> findByRecipientMemberIdOrderByCreatedAtDesc(Long recipientMemberId, Pageable pageable);

    /**
     * 멱등 삽입(phase 5b). {@code dedup_key} 유니크키 충돌 시 무시한다(ON CONFLICT DO NOTHING).
     * type 은 VARCHAR 컬럼이라 enum 이름(String)을 그대로 바인딩한다.
     */
    @Modifying
    @Query(value = "INSERT INTO notifications "
            + "(recipient_member_id, type, message, created_at, dedup_key) "
            + "VALUES (:recipientMemberId, :type, :message, :createdAt, :dedupKey) "
            + "ON CONFLICT (dedup_key) DO NOTHING", nativeQuery = true)
    void insertIgnoringDuplicate(@Param("recipientMemberId") Long recipientMemberId, @Param("type") String type,
            @Param("message") String message, @Param("createdAt") LocalDateTime createdAt, @Param("dedupKey") String dedupKey);
}
