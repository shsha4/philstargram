package com.study.philstargram.feed.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.member.application.MemberSummary;
import com.study.philstargram.post.application.PostCreatedEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedFanOutOnPostCreatedTest {

    @Mock
    FollowQueryService followQueryService;

    @Mock
    MemberQueryService memberQueryService;

    @Mock
    FeedRepository feedRepository;

    @InjectMocks
    FeedFanOutOnPostCreated feedFanOutOnPostCreated;

    @Test
    void pushesAFeedEntryToEveryFollowerOfTheAuthor() {
        LocalDateTime now = LocalDateTime.now();
        PostCreatedEvent event = new PostCreatedEvent(10L, 1L, "hello", now);
        when(memberQueryService.getSummary(1L)).thenReturn(new MemberSummary(1L, "alice"));
        when(followQueryService.getFollowerIds(1L)).thenReturn(List.of(2L, 3L));

        feedFanOutOnPostCreated.on(event);

        verify(feedRepository, times(2)).save(any(FeedEntry.class));
        verify(feedRepository).save(argThatEntryFor(2L));
        verify(feedRepository).save(argThatEntryFor(3L));
    }

    private static FeedEntry argThatEntryFor(Long ownerMemberId) {
        return org.mockito.ArgumentMatchers.argThat(entry ->
                entry.getOwnerMemberId().equals(ownerMemberId)
                        && entry.getAuthorId().equals(1L)
                        && entry.getAuthorNickname().equals("alice")
                        && entry.getContentPreview().equals("hello"));
    }
}
