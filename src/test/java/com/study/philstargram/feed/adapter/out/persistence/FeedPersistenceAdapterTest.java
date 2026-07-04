package com.study.philstargram.feed.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
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
@Import(FeedPersistenceAdapter.class)
@Testcontainers
class FeedPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    FeedRepository feedRepository;

    @Test
    void findsRecentEntriesForOwnerInDescendingOrder() {
        LocalDateTime now = LocalDateTime.now();
        feedRepository.save(FeedEntry.create(1L, 10L, 2L, "alice", "first", now.minusMinutes(1)));
        feedRepository.save(FeedEntry.create(1L, 11L, 3L, "bob", "second", now));
        feedRepository.save(FeedEntry.create(9L, 12L, 2L, "alice", "not mine", now));

        List<FeedEntry> feed = feedRepository.findRecentByOwnerMemberId(1L, 20);

        assertThat(feed).extracting(FeedEntry::getContentPreview).containsExactly("second", "first");
    }

    @Test
    void 같은_owner_post_는_중복_저장돼도_한_건만_남는다() {
        LocalDateTime now = LocalDateTime.now();
        // at-least-once 재전달 시 같은 (owner, post) 팬아웃이 두 번 와도 멱등(phase 5b).
        feedRepository.save(FeedEntry.create(5L, 100L, 2L, "alice", "hello", now));
        feedRepository.save(FeedEntry.create(5L, 100L, 2L, "alice", "hello", now));

        assertThat(feedRepository.findRecentByOwnerMemberId(5L, 20)).hasSize(1);
    }
}
