package com.study.philstargram.follow.application;

import com.study.philstargram.follow.domain.FollowRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 모듈(feed 등)에 노출하는 공개 조회 API.
 * {@code follow.domain}/{@code follow.adapter.out.persistence} 타입을 직접 노출하지 않는다.
 *
 * <p><b>셀럽 판별(phase 5c):</b> follower-count(Kafka Streams KTable)를 {@link FollowerCountView}
 * 로 조회해 임계값 이상이면 셀럽으로 본다. 하이브리드 팬아웃이 이걸로 쓰기/읽기 시점을 분기한다.
 */
@Service
public class FollowQueryService {

    private final FollowRepository followRepository;
    private final FollowerCountView followerCountView;
    private final long celebThreshold;

    public FollowQueryService(FollowRepository followRepository, FollowerCountView followerCountView,
            @Value("${philstargram.follow.celeb-threshold}") long celebThreshold) {
        this.followRepository = followRepository;
        this.followerCountView = followerCountView;
        this.celebThreshold = celebThreshold;
    }

    @Transactional(readOnly = true)
    public List<Long> getFollowerIds(Long memberId) {
        return followRepository.findFollowerIdsByFolloweeId(memberId);
    }

    /** 팔로워 수가 임계값 이상인가(셀럽). 판별 불가(스토어 미준비) 시 false. */
    public boolean isCeleb(Long memberId) {
        return followerCountView.countFor(memberId).map(count -> count >= celebThreshold).orElse(false);
    }

    /** 현재 집계된 팔로워 수. 판별 불가 시 0. */
    public long followerCount(Long memberId) {
        return followerCountView.countFor(memberId).orElse(0L);
    }

    /** 이 사용자가 팔로우하는 대상 중 셀럽만. feed 읽기 시점 pull 대상이 된다. */
    @Transactional(readOnly = true)
    public List<Long> getCelebFolloweeIds(Long followerId) {
        return followRepository.findFolloweeIdsByFollowerId(followerId).stream()
                .filter(this::isCeleb)
                .toList();
    }
}
