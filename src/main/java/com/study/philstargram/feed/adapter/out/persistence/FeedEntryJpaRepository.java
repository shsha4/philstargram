package com.study.philstargram.feed.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface FeedEntryJpaRepository extends JpaRepository<FeedEntryJpaEntity, Long> {

    List<FeedEntryJpaEntity> findByOwnerMemberIdOrderByCreatedAtDesc(Long ownerMemberId, Pageable pageable);
}
