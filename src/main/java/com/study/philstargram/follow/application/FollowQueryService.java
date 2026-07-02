package com.study.philstargram.follow.application;

import com.study.philstargram.follow.domain.FollowRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 모듈(feed 등)에 노출하는 공개 조회 API.
 * {@code follow.domain}/{@code follow.adapter.out.persistence} 타입을 직접 노출하지 않는다.
 */
@Service
public class FollowQueryService {

    private final FollowRepository followRepository;

    public FollowQueryService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public List<Long> getFollowerIds(Long memberId) {
        return followRepository.findFollowerIdsByFolloweeId(memberId);
    }
}
