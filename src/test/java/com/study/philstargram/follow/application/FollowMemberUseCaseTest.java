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
        when(memberQueryService.existsById(1L)).thenReturn(true);
        when(memberQueryService.existsById(2L)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        followMemberUseCase.execute(new FollowMemberCommand(1L, 2L));

        verify(followRepository).save(any(Follow.class));
        verify(eventPublisher).publishEvent(any(MemberFollowedEvent.class));
    }

    @Test
    void rejectsWhenFollowerDoesNotExist() {
        when(memberQueryService.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> followMemberUseCase.execute(new FollowMemberCommand(1L, 2L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsWhenFolloweeDoesNotExist() {
        when(memberQueryService.existsById(1L)).thenReturn(true);
        when(memberQueryService.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> followMemberUseCase.execute(new FollowMemberCommand(1L, 2L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsWhenAlreadyFollowing() {
        when(memberQueryService.existsById(1L)).thenReturn(true);
        when(memberQueryService.existsById(2L)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> followMemberUseCase.execute(new FollowMemberCommand(1L, 2L)))
                .isInstanceOf(DuplicateException.class);
    }
}
