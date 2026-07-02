package com.study.philstargram.notification.adapter.in.web;

import com.study.philstargram.common.response.ApiResponse;
import com.study.philstargram.notification.application.GetMyNotificationsUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/{memberId}/notifications")
public class NotificationController {

    private final GetMyNotificationsUseCase getMyNotificationsUseCase;

    public NotificationController(GetMyNotificationsUseCase getMyNotificationsUseCase) {
        this.getMyNotificationsUseCase = getMyNotificationsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(@PathVariable Long memberId) {
        List<NotificationResponse> notifications = getMyNotificationsUseCase.execute(memberId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
}
