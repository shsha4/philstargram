package com.study.philstargram.feed.adapter.out.persistence;

import com.study.philstargram.feed.domain.FeedEntry;
import com.study.philstargram.feed.domain.FeedRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class FeedPersistenceAdapter implements FeedRepository {

    private final FeedEntryJpaRepository feedEntryJpaRepository;

    FeedPersistenceAdapter(FeedEntryJpaRepository feedEntryJpaRepository) {
        this.feedEntryJpaRepository = feedEntryJpaRepository;
    }

    @Override
    public void save(FeedEntry feedEntry) {
        // 멱등 삽입: 중복 팬아웃((owner, post) 충돌)은 무시된다(phase 5b).
        feedEntryJpaRepository.insertIgnoringDuplicate(feedEntry.getOwnerMemberId(), feedEntry.getPostId(),
                feedEntry.getAuthorId(), feedEntry.getAuthorNickname(), feedEntry.getContentPreview(), feedEntry.getCreatedAt());
    }

    @Override
    public List<FeedEntry> findRecentByOwnerMemberId(Long ownerMemberId, int limit) {
        return feedEntryJpaRepository.findByOwnerMemberIdOrderByCreatedAtDesc(ownerMemberId, PageRequest.of(0, limit)).stream()
                .map(FeedPersistenceAdapter::toDomain)
                .toList();
    }

    private static FeedEntry toDomain(FeedEntryJpaEntity entity) {
        return FeedEntry.reconstitute(entity.getId(), entity.getOwnerMemberId(), entity.getPostId(),
                entity.getAuthorId(), entity.getAuthorNickname(), entity.getContentPreview(), entity.getCreatedAt());
    }
}
