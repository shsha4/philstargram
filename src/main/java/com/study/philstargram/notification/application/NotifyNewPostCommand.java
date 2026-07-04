package com.study.philstargram.notification.application;

import java.time.LocalDateTime;

public record NotifyNewPostCommand(Long postId, Long authorId, String authorNickname, LocalDateTime createdAt) {
}
