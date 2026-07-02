package com.study.philstargram.follow.application;

import com.study.philstargram.common.exception.DuplicateException;
import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.follow.domain.Follow;
import com.study.philstargram.follow.domain.FollowRepository;
import com.study.philstargram.member.application.MemberQueryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowMemberUseCase {

    private final FollowRepository followRepository;
    private final MemberQueryService memberQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public FollowMemberUseCase(FollowRepository followRepository, MemberQueryService memberQueryService, ApplicationEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.memberQueryService = memberQueryService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(FollowMemberCommand command) {
        if (!memberQueryService.existsById(command.followerId())) {
            throw new NotFoundException("존재하지 않는 회원입니다: " + command.followerId());
        }
        if (!memberQueryService.existsById(command.followeeId())) {
            throw new NotFoundException("존재하지 않는 회원입니다: " + command.followeeId());
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(command.followerId(), command.followeeId())) {
            throw new DuplicateException("이미 팔로우 중입니다.");
        }
        Follow follow = followRepository.save(Follow.create(command.followerId(), command.followeeId()));
        eventPublisher.publishEvent(new MemberFollowedEvent(follow.getFollowerId(), follow.getFolloweeId(), follow.getFollowedAt()));
    }
}
