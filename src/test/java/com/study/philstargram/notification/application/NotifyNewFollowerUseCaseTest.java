package com.study.philstargram.notification.application;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.member.application.MemberSummary;
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
    MemberQueryService memberQueryService;

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotifyNewFollowerUseCase notifyNewFollowerUseCase;

    @Test
    void notifiesTheFollowee() {
        when(memberQueryService.getSummary(2L)).thenReturn(new MemberSummary(2L, "bob"));

        notifyNewFollowerUseCase.execute(new NotifyNewFollowerCommand(2L, 1L, LocalDateTime.now()));

        verify(notificationRepository).save(argThat(n ->
                n.getRecipientMemberId().equals(1L) && n.getMessage().contains("bob")));
    }
}
