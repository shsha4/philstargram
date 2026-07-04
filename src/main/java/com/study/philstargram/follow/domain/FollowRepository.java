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

    /**
     * feed 의 읽기 시점 pull(하이브리드 팬아웃, phase 5c)에서 사용한다: 이 사용자가 팔로우하는
     * 대상 목록. 그중 셀럽만 골라 최근 글을 읽기 시점에 당겨온다.
     */
    List<Long> findFolloweeIdsByFollowerId(Long followerId);
}
