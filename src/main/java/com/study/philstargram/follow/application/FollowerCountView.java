package com.study.philstargram.follow.application;

import java.util.Optional;

/**
 * follower-count 조회 아웃바운드 포트(phase 5c). 구현은 {@code adapter.out.stream} 의 Kafka Streams
 * 상태 저장소(interactive query)다. application 은 Streams 타입을 모르고 이 포트만 안다.
 *
 * <p><b>결과적 일관성:</b> 카운트는 Kafka 이벤트(member.followed/unfollowed)로 갱신되므로 DB 와
 * 약간의 지연이 있다. 셀럽 판별은 근사치로 충분하다. 스토어 미준비(기동/리밸런스) 시
 * {@link Optional#empty()} 를 반환해 호출측이 "판별 불가 → 셀럽 아님"으로 안전하게 처리하게 한다.
 */
public interface FollowerCountView {

    Optional<Long> countFor(Long memberId);
}
