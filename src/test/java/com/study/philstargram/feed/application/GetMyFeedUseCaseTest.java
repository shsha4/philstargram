package com.study.philstargram.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMyFeedUseCaseTest {

    @Mock
    FeedRepository feedRepository;

    @Mock
    FeedCache feedCache;

    @InjectMocks
    GetMyFeedUseCase getMyFeedUseCase;

    @Test
    void 캐시_미스면_Postgres_에서_읽어_반환하고_캐시에_적재한다() {
        LocalDateTime now = LocalDateTime.now();
        when(feedCache.getRecent(1L, 20)).thenReturn(Optional.empty());
        when(feedRepository.findRecentByOwnerMemberId(1L, 20)).thenReturn(List.of(
                FeedEntry.reconstitute(100L, 1L, 10L, 2L, "alice", "hi", now),
                FeedEntry.reconstitute(101L, 1L, 11L, 3L, "bob", "hello", now)
        ));

        List<FeedItem> feed = getMyFeedUseCase.execute(1L);

        assertThat(feed).hasSize(2);
        assertThat(feed.get(0).authorNickname()).isEqualTo("alice");
        assertThat(feed.get(1).authorNickname()).isEqualTo("bob");
        verify(feedCache).put(1L, feed, 20);
    }

    @Test
    void 캐시_히트면_Postgres_를_읽지_않고_캐시값을_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        List<FeedItem> cached = List.of(new FeedItem(10L, 2L, "alice", "hi", now));
        when(feedCache.getRecent(1L, 20)).thenReturn(Optional.of(cached));

        List<FeedItem> feed = getMyFeedUseCase.execute(1L);

        assertThat(feed).isEqualTo(cached);
        verify(feedRepository, never()).findRecentByOwnerMemberId(anyLong(), anyInt());
        verify(feedCache, never()).put(anyLong(), anyList(), anyInt());
    }
}
