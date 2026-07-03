package com.study.philstargram.follow.application;

import java.time.LocalDateTime;
import org.springframework.modulith.events.Externalized;

/**
 * 팔로우 관계가 생성될 때 발행된다. follow 는 누가 이 이벤트에 반응하는지 알지 못한다.
 * notification 이 팔로위(followee)에게 알림을 보내는 데 사용한다(phase 2).
 *
 * <p><b>event-carried state (phase 4):</b> 팔로워 {@code followerNickname} 을 페이로드에 실어
 * notification 컨슈머가 "OO님이 회원님을 팔로우했습니다" 문구를 만들 때 member 를 동기 호출하지
 * 않게 한다. follow 가 팔로우 시점에 한 번 조회해 이벤트에 담는다.
 *
 * <p><b>externalization (phase 4):</b> Kafka 토픽 {@code member.followed} 로 내보낸다. 파티션
 * 키는 팔로위 id 라, 같은 사람에게 도착하는 팔로우 이벤트의 순서가 보장된다.
 */
@Externalized("member.followed::#{#this.followeeId().toString()}")
public record MemberFollowedEvent(Long followerId, Long followeeId, String followerNickname, LocalDateTime followedAt) {
}
