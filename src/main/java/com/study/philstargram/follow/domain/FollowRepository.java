package com.study.philstargram.follow.domain;

import java.util.List;

public interface FollowRepository {

    Follow save(Follow follow);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /**
     * feed 의 쓰기 시점 팬아웃에서 사용한다: 작성자가 주어졌을 때, 이 게시글을 누구에게
     * 밀어넣어야 하는가?
     */
    List<Long> findFollowerIdsByFolloweeId(Long followeeId);
}
