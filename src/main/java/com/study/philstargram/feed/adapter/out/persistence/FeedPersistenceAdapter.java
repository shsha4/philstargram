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
    public FeedEntry save(FeedEntry feedEntry) {
        return toDomain(feedEntryJpaRepository.save(toEntity(feedEntry)));
    }

    @Override
    public List<FeedEntry> findRecentByOwnerMemberId(Long ownerMemberId, int limit) {
        return feedEntryJpaRepository.findByOwnerMemberIdOrderByCreatedAtDesc(ownerMemberId, PageRequest.of(0, limit)).stream()
                .map(FeedPersistenceAdapter::toDomain)
                .toList();
    }

    private static FeedEntryJpaEntity toEntity(FeedEntry feedEntry) {
        return new FeedEntryJpaEntity(feedEntry.getId(), feedEntry.getOwnerMemberId(), feedEntry.getPostId(),
                feedEntry.getAuthorId(), feedEntry.getAuthorNickname(), feedEntry.getContentPreview(), feedEntry.getCreatedAt());
    }

    private static FeedEntry toDomain(FeedEntryJpaEntity entity) {
        return FeedEntry.reconstitute(entity.getId(), entity.getOwnerMemberId(), entity.getPostId(),
                entity.getAuthorId(), entity.getAuthorNickname(), entity.getContentPreview(), entity.getCreatedAt());
    }
}
