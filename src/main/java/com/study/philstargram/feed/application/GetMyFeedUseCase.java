package com.study.philstargram.feed.application;

import com.study.philstargram.feed.domain.FeedRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FeedFanOutOnPostCreated} 가 구성해 둔, 미리 저장된(materialized) 피드를 읽는다.
 * 설계상 조회 시점에는 다른 모듈을 호출하지 않는다(feed 의 package-info 참고).
 */
@Service
public class GetMyFeedUseCase {

    private static final int DEFAULT_FEED_SIZE = 20;

    private final FeedRepository feedRepository;

    public GetMyFeedUseCase(FeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    @Transactional(readOnly = true)
    public List<FeedItem> execute(Long memberId) {
        return feedRepository.findRecentByOwnerMemberId(memberId, DEFAULT_FEED_SIZE).stream()
                .map(FeedItem::from)
                .toList();
    }
}
