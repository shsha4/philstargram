package com.study.philstargram.feed.domain;

import java.util.List;

public interface FeedRepository {

    FeedEntry save(FeedEntry feedEntry);

    List<FeedEntry> findRecentByOwnerMemberId(Long ownerMemberId, int limit);
}
