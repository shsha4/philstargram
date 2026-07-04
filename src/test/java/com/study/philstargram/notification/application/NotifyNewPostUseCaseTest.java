package com.study.philstargram.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.follow.application.FollowQueryService;
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
    NotificationRepository notificationRepository;

    @InjectMocks
    NotifyNewPostUseCase notifyNewPostUseCase;

    @Test
    void notifiesEveryFollowerOfTheAuthor() {
        when(followQueryService.getFollowerIds(1L)).thenReturn(List.of(2L, 3L));

        // 작성자 닉네임("alice")은 이벤트가 실어온 값 — member 조회 없이 알림 문구를 만든다.
        notifyNewPostUseCase.execute(new NotifyNewPostCommand(10L, 1L, "alice", LocalDateTime.now()));

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository).save(argThat(n ->
                n.getRecipientMemberId().equals(2L) && n.getMessage().contains("alice")
                        && n.getDedupKey().equals("NEW_POST:2:10")));
    }
}
