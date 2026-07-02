package com.study.philstargram.notification.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    List<NotificationJpaEntity> findByRecipientMemberIdOrderByCreatedAtDesc(Long recipientMemberId, Pageable pageable);
}
