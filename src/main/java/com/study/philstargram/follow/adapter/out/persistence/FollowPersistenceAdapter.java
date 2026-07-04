package com.study.philstargram.follow.adapter.out.persistence;

import com.study.philstargram.follow.domain.Follow;
import com.study.philstargram.follow.domain.FollowId;
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
        // DB 가 생성한 식별자를 애그리거트에 되돌려 부여하고, 도메인 이벤트를 품은 동일 인스턴스를
        // 반환한다(UseCase 가 pullDomainEvents 로 드레인).
        if (follow.getId() == null) {
            follow.assignId(FollowId.of(saved.getId()));
        }
        return follow;
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

    @Override
    public List<Long> findFolloweeIdsByFollowerId(Long followerId) {
        return followJpaRepository.findByFollowerId(followerId).stream()
                .map(FollowJpaEntity::getFolloweeId)
                .toList();
    }

    private static FollowJpaEntity toEntity(Follow follow) {
        Long id = follow.getId() == null ? null : follow.getId().value();
        return new FollowJpaEntity(id, follow.getFollowerId(), follow.getFolloweeId(), follow.getFollowedAt());
    }
}
