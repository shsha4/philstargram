package com.study.philstargram.feed.application;

import java.util.List;
import java.util.Optional;

/**
 * 피드 읽기 캐시의 아웃바운드 포트(phase 5a). 구현은 {@code adapter.out.cache} 의 Redis 어댑터다.
 *
 * <p><b>일관성 모델:</b> 진실의 원천은 Postgres {@code feed_entries}, 이 캐시는 버려도 되는(disposable)
 * 사본이다. 그래서 다음 원칙을 따른다.
 * <ul>
 *   <li><b>읽기(cache-aside):</b> {@link #getRecent} 가 미스면 UseCase 가 Postgres 를 읽어
 *       {@link #put} 으로 재적재한다.</li>
 *   <li><b>쓰기(write-through-if-present):</b> 팬아웃은 Postgres 에 먼저 저장하고,
 *       해당 사용자의 캐시가 <b>이미 있을 때만</b> {@link #appendIfPresent} 로 한 건 덧붙인다
 *       (콜드 캐시는 굳이 채우지 않아 쓰기 증폭/메모리 낭비를 피한다).</li>
 *   <li><b>실패 시 fail-safe:</b> 캐시 조작이 실패하면 예외를 삼키고(요청을 막지 않음) 필요하면
 *       {@link #evict} 로 무효화한다 → 다음 읽기에서 Postgres 로 자가 치유된다(bounded staleness).</li>
 * </ul>
 * 즉 캐시 장애나 이중 쓰기 유실이 있어도 잘못된 데이터를 영구히 서빙하지 않는다.
 */
public interface FeedCache {

    /**
     * 캐시된 최신 피드를 반환한다. 캐시 미스(키 없음)면 {@link Optional#empty()} — UseCase 가 이때
     * Postgres 를 읽어 {@link #put} 으로 재적재한다.
     */
    Optional<List<FeedItem>> getRecent(Long memberId, int limit);

    /** 캐시 미스 후 Postgres 에서 읽은 결과로 캐시를 재적재한다(상위 {@code limit} 개, TTL 부여). */
    void put(Long memberId, List<FeedItem> items, int limit);

    /** 캐시가 이미 존재할 때만 한 건을 덧붙이고 상위 {@code limit} 개로 트림한다(write-through). */
    void appendIfPresent(Long memberId, FeedItem item, int limit);

    /** 캐시를 무효화한다(fail-safe). */
    void evict(Long memberId);
}
