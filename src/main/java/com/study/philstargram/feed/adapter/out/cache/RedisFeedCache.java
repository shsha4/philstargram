package com.study.philstargram.feed.adapter.out.cache;

import com.study.philstargram.feed.application.FeedCache;
import com.study.philstargram.feed.application.FeedItem;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link FeedCache} 의 Redis 구현(phase 5a). 사용자별 타임라인을 <b>ZSET</b>
 * {@code feed:{memberId}} 로 캐시한다 — score = createdAt epoch millis, member = {@link FeedItem}
 * 을 직렬화한 JSON 문자열. 최신순 조회는 {@code ZREVRANGE} 다.
 *
 * <p><b>캐시는 절대 요청을 깨지 않는다:</b> 모든 Redis 접근을 감싸 예외를 삼킨다. 읽기 실패는
 * 미스로 간주해 Postgres 폴백을 유도하고, 쓰기 실패는 키를 무효화(evict)해 다음 읽기에서
 * 재적재되게 한다(fail-safe). 진실의 원천은 Postgres 이므로 이 저하(degradation)는 항상 안전하다.
 *
 * <p><b>빈 피드 주의:</b> Redis ZSET 은 멤버가 0개면 키 자체가 사라진다. 따라서 "게시글이 하나도
 * 없는 피드"는 캐시로 표현되지 않고 매번 미스(→ 저렴한 Postgres 조회)가 된다. 의도된 트레이드오프.
 */
@Component
class RedisFeedCache implements FeedCache {

    private static final Logger log = LoggerFactory.getLogger(RedisFeedCache.class);
    private static final String KEY_PREFIX = "feed:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    RedisFeedCache(StringRedisTemplate redis, ObjectMapper objectMapper,
            @Value("${philstargram.feed.cache.ttl:PT10M}") Duration ttl) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<List<FeedItem>> getRecent(Long memberId, int limit) {
        String key = key(memberId);
        try {
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                return Optional.empty(); // 미스 → UseCase 가 Postgres 로 폴백/재적재
            }
            Set<String> raw = redis.opsForZSet().reverseRange(key, 0, limit - 1);
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(raw.stream().map(this::deserialize).toList());
        } catch (RuntimeException e) {
            log.warn("피드 캐시 읽기 실패(미스로 처리): key={}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(Long memberId, List<FeedItem> items, int limit) {
        if (items.isEmpty()) {
            return; // 빈 피드는 캐시하지 않는다(위 주의 참고)
        }
        String key = key(memberId);
        try {
            Set<TypedTuple<String>> tuples = items.stream()
                    .map(item -> TypedTuple.of(serialize(item), score(item)))
                    .collect(java.util.stream.Collectors.toSet());
            redis.delete(key);
            redis.opsForZSet().add(key, tuples);
            trim(key, limit);
            redis.expire(key, ttl);
        } catch (RuntimeException e) {
            log.warn("피드 캐시 적재 실패(무효화): key={}", key, e);
            evict(memberId);
        }
    }

    @Override
    public void appendIfPresent(Long memberId, FeedItem item, int limit) {
        String key = key(memberId);
        try {
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                return; // 콜드 캐시는 채우지 않는다(lazy) — 다음 읽기 시 put 으로 적재된다
            }
            redis.opsForZSet().add(key, serialize(item), score(item));
            trim(key, limit);
            redis.expire(key, ttl);
        } catch (RuntimeException e) {
            log.warn("피드 캐시 append 실패(무효화): key={}", key, e);
            evict(memberId);
        }
    }

    @Override
    public void evict(Long memberId) {
        try {
            redis.delete(key(memberId));
        } catch (RuntimeException e) {
            log.warn("피드 캐시 무효화 실패: memberId={}", memberId, e);
        }
    }

    /** 점수(최신순) 상위 {@code limit} 개만 남기고 나머지(낮은 점수)를 제거한다. */
    private void trim(String key, int limit) {
        ZSetOperations<String, String> ops = redis.opsForZSet();
        ops.removeRange(key, 0, -(limit + 1L));
    }

    private double score(FeedItem item) {
        return item.createdAt().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private String serialize(FeedItem item) {
        return objectMapper.writeValueAsString(item);
    }

    private FeedItem deserialize(String json) {
        return objectMapper.readValue(json, FeedItem.class);
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
