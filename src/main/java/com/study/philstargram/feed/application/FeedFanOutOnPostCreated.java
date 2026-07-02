package com.study.philstargram.feed.application;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.member.application.MemberQueryService;
import com.study.philstargram.post.application.PostCreatedEvent;
import java.util.List;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 쓰기 시점 팬아웃(fan-out-on-write): {@link PostCreatedEvent} 에 반응하여 작성자를
 * 팔로우하는 모든 사용자의 피드에 {@link FeedEntry} 를 저장한다. 발행 트랜잭션이 커밋된
 * 이후 별도의 트랜잭션에서 실행되므로({@link ApplicationModuleListener} 참고), 팬아웃이
 * 느리거나 실패하더라도 게시글 생성을 막거나 롤백시키지 않는다.
 */
@Component
class FeedFanOutOnPostCreated {

    private final FollowQueryService followQueryService;
    private final MemberQueryService memberQueryService;
    private final FeedRepository feedRepository;

    FeedFanOutOnPostCreated(FollowQueryService followQueryService, MemberQueryService memberQueryService, FeedRepository feedRepository) {
        this.followQueryService = followQueryService;
        this.memberQueryService = memberQueryService;
        this.feedRepository = feedRepository;
    }

    @ApplicationModuleListener
    void on(PostCreatedEvent event) {
        String authorNickname = memberQueryService.getSummary(event.authorId()).nickname();
        List<Long> followerIds = followQueryService.getFollowerIds(event.authorId());
        for (Long followerId : followerIds) {
            feedRepository.save(FeedEntry.create(
                    followerId, event.postId(), event.authorId(), authorNickname, event.contentPreview(), event.createdAt()));
        }
    }
}
