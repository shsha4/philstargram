package com.study.philstargram.follow.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.DuplicateException;
import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.follow.domain.Follow;
import com.study.philstargram.follow.domain.FollowRepository;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.member.application.MemberSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FollowMemberUseCaseTest {

    @Mock
    FollowRepository followRepository;

    @Mock
    MemberQueryService memberQueryService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    FollowMemberUseCase followMemberUseCase;

    @Test
    void followsWhenBothMembersExistAndNotAlreadyFollowing() {
        // 팔로워는 getSummary 로 존재 검증 + 닉네임 확보(event-carried state), 팔로위는 existsById 로 검증.
        when(memberQueryService.getSummary(1L)).thenReturn(new MemberSummary(1L, "alice"));
        when(memberQueryService.existsById(2L)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        followMemberUseCase.execute(new FollowMemberCommand(1L, 2L));

        verify(followRepository).save(any(Follow.class));
        verify(eventPublisher).publishEvent(any(MemberFollowedEvent.class));
    }

    @Test
    void rejectsWhenFollowerDoesNotExist() {
        when(memberQueryService.getSummary(1L)).thenThrow(new NotFoundException("존재하지 않는 회원입니다: 1"));

        assertThatThrownBy(() -> followMemberUseCase.execute(new FollowMemberCommand(1L, 2L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsWhenFolloweeDoesNotExist() {
        when(memberQueryService.getSummary(1L)).thenReturn(new MemberSummary(1L, "alice"));
        when(memberQueryService.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> followMemberUseCase.execute(new FollowMemberCommand(1L, 2L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsWhenAlreadyFollowing() {
        when(memberQueryService.getSummary(1L)).thenReturn(new MemberSummary(1L, "alice"));
        when(memberQueryService.existsById(2L)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> followMemberUseCase.execute(new FollowMemberCommand(1L, 2L)))
                .isInstanceOf(DuplicateException.class);
    }
}
