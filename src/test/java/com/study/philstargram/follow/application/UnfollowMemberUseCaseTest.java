package com.study.philstargram.follow.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.common.exception.NotFoundException;
import com.study.philstargram.follow.domain.FollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnfollowMemberUseCaseTest {

    @Mock
    FollowRepository followRepository;

    @InjectMocks
    UnfollowMemberUseCase unfollowMemberUseCase;

    @Test
    void unfollowsWhenRelationExists() {
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(true);

        unfollowMemberUseCase.execute(new UnfollowMemberCommand(1L, 2L));

        verify(followRepository).deleteByFollowerIdAndFolloweeId(1L, 2L);
    }

    @Test
    void throwsWhenRelationDoesNotExist() {
        when(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> unfollowMemberUseCase.execute(new UnfollowMemberCommand(1L, 2L)))
                .isInstanceOf(NotFoundException.class);
    }
}
