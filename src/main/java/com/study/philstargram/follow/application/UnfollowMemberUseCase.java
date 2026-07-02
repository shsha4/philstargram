package com.study.philstargram.follow.application;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.follow.domain.FollowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnfollowMemberUseCase {

    private final FollowRepository followRepository;

    public UnfollowMemberUseCase(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Transactional
    public void execute(UnfollowMemberCommand command) {
        if (!followRepository.existsByFollowerIdAndFolloweeId(command.followerId(), command.followeeId())) {
            throw new NotFoundException("팔로우 관계가 존재하지 않습니다.");
        }
        followRepository.deleteByFollowerIdAndFolloweeId(command.followerId(), command.followeeId());
    }
}
