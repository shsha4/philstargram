package com.study.philstargram.post.application;

import com.study.philstargram.post.domain.PostRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 모듈(feed 등)에 노출하는 공개 조회 API(phase 5c). 도메인/영속 타입을 직접 노출하지 않는다.
 * feed 의 읽기 시점 pull(하이브리드 팬아웃)에서 셀럽 작성자들의 최근 글을 당겨오는 데 쓰인다.
 */
@Service
public class PostQueryService {

    private final PostRepository postRepository;

    public PostQueryService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<RecentPostSummary> getRecentByAuthors(List<Long> authorIds, int limit) {
        return postRepository.findRecentByAuthorIds(authorIds, limit).stream()
                .map(RecentPostSummary::from)
                .toList();
    }
}
