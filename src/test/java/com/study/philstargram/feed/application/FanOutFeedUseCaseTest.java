package com.study.philstargram.feed.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import com.study.philstargram.follow.application.FollowQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FanOutFeedUseCaseTest {

    @Mock
    FollowQueryService followQueryService;

    @Mock
    FeedRepository feedRepository;

    @Mock
    FeedCache feedCache;

    @InjectMocks
    FanOutFeedUseCase fanOutFeedUseCase;

    @Test
    void pushesAFeedEntryToEveryFollowerOfTheAuthor() {
        LocalDateTime now = LocalDateTime.now();
        when(followQueryService.getFollowerIds(1L)).thenReturn(List.of(2L, 3L));

        // 작성자 닉네임("alice")은 이벤트가 실어온 값(event-carried state) — member 조회 없이 사용한다.
        fanOutFeedUseCase.execute(new FanOutFeedCommand(10L, 1L, "alice", "hello", now));

        verify(feedRepository, times(2)).save(any(FeedEntry.class));
        verify(feedRepository).save(argThat(entry ->
                entry.getOwnerMemberId().equals(2L)
                        && entry.getAuthorId().equals(1L)
                        && entry.getAuthorNickname().equals("alice")
                        && entry.getContentPreview().equals("hello")));
        verify(feedRepository).save(argThat(entry -> entry.getOwnerMemberId().equals(3L)));
    }

    @Test
    void 셀럽_작성자면_쓰기_팬아웃을_건너뛴다() {
        // 셀럽은 읽기 시점 pull 로 처리 — 쓰기 팬아웃(피드 저장/캐시 append)을 하지 않는다(phase 5c).
        when(followQueryService.isCeleb(1L)).thenReturn(true);

        fanOutFeedUseCase.execute(new FanOutFeedCommand(10L, 1L, "celeb", "hello", LocalDateTime.now()));

        verify(followQueryService, never()).getFollowerIds(anyLong());
        verify(feedRepository, never()).save(any(FeedEntry.class));
        verify(feedCache, never()).appendIfPresent(anyLong(), any(FeedItem.class), anyInt());
    }

    @Test
    void 팔로워별로_캐시가_있을때만_덧붙이도록_appendIfPresent_를_호출한다() {
        LocalDateTime now = LocalDateTime.now();
        when(followQueryService.getFollowerIds(1L)).thenReturn(List.of(2L, 3L));

        fanOutFeedUseCase.execute(new FanOutFeedCommand(10L, 1L, "alice", "hello", now));

        verify(feedCache, times(2)).appendIfPresent(anyLong(), any(FeedItem.class), anyInt());
        verify(feedCache).appendIfPresent(eq(2L), argThat(item ->
                item.postId().equals(10L) && item.authorNickname().equals("alice")), eq(20));
        verify(feedCache).appendIfPresent(eq(3L), any(FeedItem.class), eq(20));
    }
}
