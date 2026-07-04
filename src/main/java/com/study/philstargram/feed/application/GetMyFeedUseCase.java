package com.study.philstargram.feed.application;

import com.study.philstargram.feed.domain.FeedRepository;
import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.post.application.PostQueryService;
import com.study.philstargram.post.application.RecentPostSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 타임라인을 조회한다.
 *
 * <p><b>캐시(phase 5a):</b> materialized 피드는 cache-aside 로 읽는다 — {@link FeedCache} 미스 시
 * Postgres({@link FeedRepository}, 진실의 원천)를 읽어 재적재.
 *
 * <p><b>하이브리드 팬아웃 읽기 병합(phase 5c):</b> 셀럽 작성자의 글은 쓰기 시점에 팬아웃되지 않으므로
 * (FanOutFeedUseCase 참고) materialized 피드에 없다. 그래서 조회 시 리더가 팔로우한 셀럽들의 최근
 * 글을 {@link PostQueryService} 로 <b>읽기 시점에 pull</b> 해 materialized 피드와 병합한다
 * (createdAt 최신순, postId 중복 제거, 상위 N). 셀럽을 팔로우하지 않으면 병합 비용은 0.
 */
@Service
public class GetMyFeedUseCase {

    private static final int DEFAULT_FEED_SIZE = 20;

    private final FeedRepository feedRepository;
    private final FeedCache feedCache;
    private final FollowQueryService followQueryService;
    private final PostQueryService postQueryService;

    public GetMyFeedUseCase(FeedRepository feedRepository, FeedCache feedCache,
            FollowQueryService followQueryService, PostQueryService postQueryService) {
        this.feedRepository = feedRepository;
        this.feedCache = feedCache;
        this.followQueryService = followQueryService;
        this.postQueryService = postQueryService;
    }

    @Transactional(readOnly = true)
    public List<FeedItem> execute(Long memberId) {
        List<FeedItem> materialized = readMaterialized(memberId);

        List<Long> celebFollowees = followQueryService.getCelebFolloweeIds(memberId);
        if (celebFollowees.isEmpty()) {
            return materialized;
        }
        List<FeedItem> celebPosts = postQueryService.getRecentByAuthors(celebFollowees, DEFAULT_FEED_SIZE).stream()
                .map(GetMyFeedUseCase::toFeedItem)
                .toList();
        return merge(materialized, celebPosts);
    }

    /** cache-aside: 캐시 미스면 Postgres 를 읽어 재적재한다. */
    private List<FeedItem> readMaterialized(Long memberId) {
        Optional<List<FeedItem>> cached = feedCache.getRecent(memberId, DEFAULT_FEED_SIZE);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<FeedItem> items = feedRepository.findRecentByOwnerMemberId(memberId, DEFAULT_FEED_SIZE).stream()
                .map(FeedItem::from)
                .toList();
        feedCache.put(memberId, items, DEFAULT_FEED_SIZE);
        return items;
    }

    /** materialized(비-셀럽) + pull(셀럽)을 최신순 병합, postId 중복 제거 후 상위 N. */
    private static List<FeedItem> merge(List<FeedItem> materialized, List<FeedItem> celebPosts) {
        Set<Long> seen = new HashSet<>();
        List<FeedItem> merged = new ArrayList<>(materialized.size() + celebPosts.size());
        for (FeedItem item : materialized) {
            if (seen.add(item.postId())) {
                merged.add(item);
            }
        }
        for (FeedItem item : celebPosts) {
            if (seen.add(item.postId())) {
                merged.add(item);
            }
        }
        return merged.stream()
                .sorted(Comparator.comparing(FeedItem::createdAt).reversed())
                .limit(DEFAULT_FEED_SIZE)
                .toList();
    }

    private static FeedItem toFeedItem(RecentPostSummary summary) {
        return new FeedItem(summary.postId(), summary.authorId(), summary.authorNickname(), summary.contentPreview(), summary.createdAt());
    }
}
