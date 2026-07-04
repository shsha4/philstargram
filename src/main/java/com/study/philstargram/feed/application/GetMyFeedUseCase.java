package com.study.philstargram.feed.application;

import com.study.philstargram.feed.domain.FeedRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팬아웃으로 미리 저장된(materialized) 피드를 읽는다. 설계상 조회 시점에는 다른 모듈을
 * 호출하지 않는다(feed 의 package-info 참고).
 *
 * <p><b>캐시(phase 5a):</b> cache-aside 로 동작한다 — 먼저 {@link FeedCache} 를 보고, 미스면
 * Postgres({@link FeedRepository}, 진실의 원천)를 읽어 캐시에 재적재한 뒤 반환한다. 캐시는
 * 장애 시 미스로 저하되므로 이 경로는 항상 안전하다.
 */
@Service
public class GetMyFeedUseCase {

    private static final int DEFAULT_FEED_SIZE = 20;

    private final FeedRepository feedRepository;
    private final FeedCache feedCache;

    public GetMyFeedUseCase(FeedRepository feedRepository, FeedCache feedCache) {
        this.feedRepository = feedRepository;
        this.feedCache = feedCache;
    }

    @Transactional(readOnly = true)
    public List<FeedItem> execute(Long memberId) {
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
}
