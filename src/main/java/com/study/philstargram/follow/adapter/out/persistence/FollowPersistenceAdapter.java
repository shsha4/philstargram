package com.study.philstargram.follow.adapter.out.persistence;

import com.study.philstargram.follow.domain.Follow;
import com.study.philstargram.follow.domain.FollowRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class FollowPersistenceAdapter implements FollowRepository {

    private final FollowJpaRepository followJpaRepository;

    FollowPersistenceAdapter(FollowJpaRepository followJpaRepository) {
        this.followJpaRepository = followJpaRepository;
    }

    @Override
    public Follow save(Follow follow) {
        FollowJpaEntity saved = followJpaRepository.save(toEntity(follow));
        return toDomain(saved);
    }

    @Override
    public boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId) {
        return followJpaRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId) {
        followJpaRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public List<Long> findFollowerIdsByFolloweeId(Long followeeId) {
        return followJpaRepository.findByFolloweeId(followeeId).stream()
                .map(FollowJpaEntity::getFollowerId)
                .toList();
    }

    private static FollowJpaEntity toEntity(Follow follow) {
        return new FollowJpaEntity(follow.getId(), follow.getFollowerId(), follow.getFolloweeId(), follow.getFollowedAt());
    }

    private static Follow toDomain(FollowJpaEntity entity) {
        return Follow.reconstitute(entity.getId(), entity.getFollowerId(), entity.getFolloweeId(), entity.getFollowedAt());
    }
}
