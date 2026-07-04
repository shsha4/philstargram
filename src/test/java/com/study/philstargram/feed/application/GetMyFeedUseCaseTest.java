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
import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.post.application.PostQueryService;
import com.study.philstargram.post.application.RecentPostSummary;
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

    @Mock
    FollowQueryService followQueryService;

    @Mock
    PostQueryService postQueryService;

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

    @Test
    void 셀럽을_팔로우하면_셀럽_최근글을_읽기시점에_pull_해서_병합한다() {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime t2 = t1.plusMinutes(10);
        // materialized(비-셀럽) 피드: 오래된 글
        when(feedCache.getRecent(1L, 20)).thenReturn(Optional.of(List.of(
                new FeedItem(10L, 2L, "normal", "old", t1))));
        // 리더가 팔로우한 셀럽 5번의 더 최신 글을 읽기 시점에 pull
        when(followQueryService.getCelebFolloweeIds(1L)).thenReturn(List.of(5L));
        when(postQueryService.getRecentByAuthors(List.of(5L), 20)).thenReturn(List.of(
                new RecentPostSummary(20L, 5L, "celeb", "new", t2)));

        List<FeedItem> feed = getMyFeedUseCase.execute(1L);

        // 최신순 병합: 셀럽 글(t2)이 먼저, 그다음 materialized(t1)
        assertThat(feed).extracting(FeedItem::postId).containsExactly(20L, 10L);
        assertThat(feed.get(0).authorNickname()).isEqualTo("celeb");
    }
}
