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
        // 팔로워 존재 검증과 닉네임 조회를 한 번에(없으면 NotFoundException). event-carried state:
        // 이 닉네임을 이벤트에 실어 notification 이 member 를 재조회하지 않고 알림 문구를 만든다.
        String followerNickname = memberQueryService.getSummary(command.followerId()).nickname();
        if (!memberQueryService.existsById(command.followeeId())) {
            throw new NotFoundException("존재하지 않는 회원입니다: " + command.followeeId());
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(command.followerId(), command.followeeId())) {
            throw new DuplicateException("이미 팔로우 중입니다.");
        }
        Follow follow = followRepository.save(Follow.create(command.followerId(), command.followeeId()));
        // 애그리거트가 발생시킨 도메인 이벤트를 드레인해 모듈 간 계약으로 번역·발행한다.
        follow.pullDomainEvents().forEach(event ->
                eventPublisher.publishEvent(new MemberFollowedEvent(event.followerId(), event.followeeId(), followerNickname, event.followedAt())));
    }
}
