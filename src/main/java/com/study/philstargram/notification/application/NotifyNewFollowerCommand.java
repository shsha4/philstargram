package com.study.philstargram.notification.application;

import java.time.LocalDateTime;

public record NotifyNewFollowerCommand(Long followerId, Long followeeId, LocalDateTime followedAt) {
}
