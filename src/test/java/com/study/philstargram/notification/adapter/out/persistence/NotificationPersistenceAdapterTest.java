package com.study.philstargram.notification.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.notification.domain.Notification;
import com.study.philstargram.notification.domain.NotificationRepository;
import com.study.philstargram.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NotificationPersistenceAdapter.class)
@Testcontainers
class NotificationPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    NotificationRepository notificationRepository;

    @Test
    void findsRecentNotificationsForRecipientInDescendingOrder() {
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.save(Notification.create(1L, NotificationType.NEW_FOLLOWER, "bob followed you", now.minusMinutes(1)));
        notificationRepository.save(Notification.create(1L, NotificationType.NEW_POST, "alice posted", now));
        notificationRepository.save(Notification.create(9L, NotificationType.NEW_POST, "not mine", now));

        List<Notification> notifications = notificationRepository.findRecentByRecipientMemberId(1L, 20);

        assertThat(notifications).extracting(Notification::getMessage).containsExactly("alice posted", "bob followed you");
    }
}
