package com.study.philstargram.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMyFeedUseCaseTest {

    @Mock
    FeedRepository feedRepository;

    @InjectMocks
    GetMyFeedUseCase getMyFeedUseCase;

    @Test
    void returnsMaterializedFeedEntriesForOwner() {
        LocalDateTime now = LocalDateTime.now();
        when(feedRepository.findRecentByOwnerMemberId(1L, 20)).thenReturn(List.of(
                FeedEntry.reconstitute(100L, 1L, 10L, 2L, "alice", "hi", now),
                FeedEntry.reconstitute(101L, 1L, 11L, 3L, "bob", "hello", now)
        ));

        List<FeedItem> feed = getMyFeedUseCase.execute(1L);

        assertThat(feed).hasSize(2);
        assertThat(feed.get(0).authorNickname()).isEqualTo("alice");
        assertThat(feed.get(1).authorNickname()).isEqualTo("bob");
    }
}
