package com.study.philstargram.follow.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    List<FollowJpaEntity> findByFolloweeId(Long followeeId);
}
