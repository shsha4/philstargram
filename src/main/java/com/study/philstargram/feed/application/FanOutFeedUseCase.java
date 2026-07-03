package com.study.philstargram.feed.application;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import com.study.philstargram.follow.application.FollowQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쓰기 시점 팬아웃(fan-out-on-write): 작성자를 팔로우하는 모든 사용자의 피드에
 * {@link FeedEntry} 를 저장한다. 작성자 닉네임/본문 미리보기를 비정규화해 함께 저장하여
 * 피드 조회 시 member/post 재호출이 필요 없게 한다.
 *
 * <p>이벤트 수신(인바운드 어댑터)과 분리된 순수 유즈케이스다 — 이벤트 리스너는
 * {@code adapter.in.event} 에서 이 유즈케이스를 호출하기만 한다(web 컨트롤러가 UseCase 를
 * 호출하는 것과 동일한 구조).
 *
 * <p><b>phase 4 결합 제거:</b> 작성자 닉네임을 이벤트가 실어오므로(event-carried state) 더 이상
 * member 를 동기 조회하지 않는다. feed 는 이제 member 에 의존하지 않고 follow 만 호출한다.
 */
@Service
public class FanOutFeedUseCase {

    private final FollowQueryService followQueryService;
    private final FeedRepository feedRepository;

    public FanOutFeedUseCase(FollowQueryService followQueryService, FeedRepository feedRepository) {
        this.followQueryService = followQueryService;
        this.feedRepository = feedRepository;
    }

    @Transactional
    public void execute(FanOutFeedCommand command) {
        for (Long followerId : followQueryService.getFollowerIds(command.authorId())) {
            feedRepository.save(FeedEntry.create(
                    followerId, command.postId(), command.authorId(), command.authorNickname(), command.contentPreview(), command.createdAt()));
        }
    }
}
