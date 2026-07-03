package com.study.philstargram.notification.application;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.study.philstargram.notification.domain.NotificationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifyNewFollowerUseCaseTest {

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotifyNewFollowerUseCase notifyNewFollowerUseCase;

    @Test
    void notifiesTheFollowee() {
        // 팔로워 닉네임("bob")은 이벤트가 실어온 값 — member 조회 없이 알림 문구를 만든다.
        notifyNewFollowerUseCase.execute(new NotifyNewFollowerCommand(2L, 1L, "bob", LocalDateTime.now()));

        verify(notificationRepository).save(argThat(n ->
                n.getRecipientMemberId().equals(1L) && n.getMessage().contains("bob")));
    }
}
