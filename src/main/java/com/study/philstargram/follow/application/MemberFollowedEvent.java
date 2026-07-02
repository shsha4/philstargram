package com.study.philstargram.follow.application;

import java.time.LocalDateTime;

/**
 * 팔로우 관계가 생성될 때 발행된다. follow 는 누가 이 이벤트에 반응하는지 알지 못한다.
 * notification 이 팔로위(followee)에게 알림을 보내는 데 사용한다(phase 2).
 */
public record MemberFollowedEvent(Long followerId, Long followeeId, LocalDateTime followedAt) {
}
