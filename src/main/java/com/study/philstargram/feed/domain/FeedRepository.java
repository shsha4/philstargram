package com.study.philstargram.feed.domain;

import java.util.List;

public interface FeedRepository {

    /**
     * 피드 엔트리를 저장한다. <b>멱등(phase 5b):</b> 같은 {@code (ownerMemberId, postId)} 가 이미 있으면
     * 무시한다(ON CONFLICT DO NOTHING). at-least-once 재전달로 팬아웃이 중복돼도 한 건만 남는다.
     */
    void save(FeedEntry feedEntry);

    List<FeedEntry> findRecentByOwnerMemberId(Long ownerMemberId, int limit);
}
