package com.study.philstargram;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.feed.application.FeedCache;
import com.study.philstargram.feed.application.FeedItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * phase 5a 피드 캐시(Redis) 통합 테스트. 실제 Redis 컨테이너(AbstractIntegrationTest 싱글턴)에 대해
 * cache-aside 읽기 / write-through-if-present 쓰기 / fail-safe evict 의 일관성 계약을 검증한다.
 *
 * <p>싱글턴 컨테이너를 여러 테스트가 공유하므로 키 충돌을 피하려고 테스트마다 별도 memberId 를 쓴다.
 */
class FeedCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    FeedCache feedCache;

    @Autowired
    StringRedisTemplate redis;

    @Test
    void 미스면_empty_적재후_히트하면_최신순으로_반환한다() {
        long memberId = 9001L;
        redis.delete("feed:" + memberId);

        // 미스
        assertThat(feedCache.getRecent(memberId, 20)).isEmpty();

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        feedCache.put(memberId, List.of(
                new FeedItem(10L, 2L, "alice", "old", base),
                new FeedItem(11L, 3L, "bob", "new", base.plusMinutes(5))), 20);

        // 히트: score(createdAt) 내림차순
        Optional<List<FeedItem>> hit = feedCache.getRecent(memberId, 20);
        assertThat(hit).isPresent();
        assertThat(hit.get()).extracting(FeedItem::postId).containsExactly(11L, 10L);
    }

    @Test
    void appendIfPresent_는_캐시가_있을때만_덧붙이고_상위N개로_트림한다() {
        long memberId = 9002L;
        redis.delete("feed:" + memberId);
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);

        // 콜드 캐시: append 해도 아무 일도 없어야 한다(lazy)
        feedCache.appendIfPresent(memberId, new FeedItem(1L, 2L, "a", "x", base), 3);
        assertThat(feedCache.getRecent(memberId, 20)).isEmpty();

        // 캐시를 만든 뒤 append: limit=3 으로 트림되어 최신 3개만 남는다
        feedCache.put(memberId, List.of(new FeedItem(1L, 2L, "a", "1", base)), 3);
        feedCache.appendIfPresent(memberId, new FeedItem(2L, 2L, "a", "2", base.plusMinutes(1)), 3);
        feedCache.appendIfPresent(memberId, new FeedItem(3L, 2L, "a", "3", base.plusMinutes(2)), 3);
        feedCache.appendIfPresent(memberId, new FeedItem(4L, 2L, "a", "4", base.plusMinutes(3)), 3);

        Optional<List<FeedItem>> hit = feedCache.getRecent(memberId, 20);
        assertThat(hit).isPresent();
        assertThat(hit.get()).extracting(FeedItem::postId).containsExactly(4L, 3L, 2L);
    }

    @Test
    void evict_하면_다시_미스가_된다() {
        long memberId = 9003L;
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        feedCache.put(memberId, List.of(new FeedItem(1L, 2L, "a", "x", base)), 20);
        assertThat(feedCache.getRecent(memberId, 20)).isPresent();

        feedCache.evict(memberId);

        assertThat(feedCache.getRecent(memberId, 20)).isEmpty();
    }
}
