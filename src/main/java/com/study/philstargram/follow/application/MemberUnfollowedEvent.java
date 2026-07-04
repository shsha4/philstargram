package com.study.philstargram.follow.application;

import java.time.LocalDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * 팔로우 관계가 해제될 때 발행되는 모듈 간 계약(phase 5c). follow 는 누가 반응하는지 알지 못한다.
 *
 * <p>도입 배경: follower-count 집계(Kafka Streams KTable)가 팔로우(+)뿐 아니라 <b>언팔로우(-)</b>도
 * 알아야 정확한 카운트를 유지하고, 그 카운트로 셀럽 여부를 판별해 하이브리드 팬아웃을 분기한다.
 * 페이로드에 닉네임 같은 event-carried state 는 필요 없다(집계는 followerId/followeeId 만 쓴다).
 *
 * <p>externalization: Kafka 토픽 {@code member.unfollowed} 로 내보낸다. 파티션 키는 팔로위 id 라
 * {@code member.followed} 와 같은 파티션 규칙을 따른다(한 사람에 대한 관계 변화의 순서 보장).
 */
@Externalized("member.unfollowed::#{#this.followeeId().toString()}")
public record MemberUnfollowedEvent(Long followerId, Long followeeId, LocalDateTime unfollowedAt) {
}
