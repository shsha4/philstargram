package com.study.philstargram.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.member.application.MemberSummary;
import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyNewPostUseCaseTest {

    @Mock
    FollowQueryService followQueryService;

    @Mock
    MemberQueryService memberQueryService;

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotifyNewPostUseCase notifyNewPostUseCase;

    @Test
    void notifiesEveryFollowerOfTheAuthor() {
        when(memberQueryService.getSummary(1L)).thenReturn(new MemberSummary(1L, "alice"));
        when(followQueryService.getFollowerIds(1L)).thenReturn(List.of(2L, 3L));

        notifyNewPostUseCase.execute(new NotifyNewPostCommand(1L, LocalDateTime.now()));

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }
}
