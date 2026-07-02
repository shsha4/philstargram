package com.study.philstargram.follow.application;

public record UnfollowMemberCommand(Long followerId, Long followeeId) {
}
