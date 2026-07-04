package com.study.philstargram.follow.application;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.follow.domain.FollowRepository;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnfollowMemberUseCase {

    private final FollowRepository followRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UnfollowMemberUseCase(FollowRepository followRepository, ApplicationEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UnfollowMemberCommand command) {
        if (!followRepository.existsByFollowerIdAndFolloweeId(command.followerId(), command.followeeId())) {
            throw new NotFoundException("팔로우 관계가 존재하지 않습니다.");
        }
        followRepository.deleteByFollowerIdAndFolloweeId(command.followerId(), command.followeeId());
        // 언팔로우는 키로만 삭제하므로 애그리거트 도메인 이벤트 대신 여기서 직접 발행한다(follow/write 는
        // 애그리거트가 raise 하는 것과의 의도적 비대칭). follower-count 집계가 -1 을 반영하는 데 쓰인다.
        eventPublisher.publishEvent(new MemberUnfollowedEvent(command.followerId(), command.followeeId(), LocalDateTime.now()));
    }
}
