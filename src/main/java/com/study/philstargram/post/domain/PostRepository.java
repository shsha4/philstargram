package com.study.philstargram.post.domain;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(PostId id);

    /**
     * 주어진 작성자들의 게시글 중 최신 {@code limit} 개를 최신순으로 모은다(하이브리드 팬아웃의
     * 읽기 시점 pull, phase 5c). 셀럽 작성자 목록에 대해 호출되며, feed 가 이를 materialized 피드와
     * 병합한다.
     */
    List<Post> findRecentByAuthorIds(List<Long> authorIds, int limit);
}
