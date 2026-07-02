package com.study.philstargram.feed.adapter.in.web;

import com.study.philstargram.common.response.ApiResponse;
import com.study.philstargram.feed.application.GetMyFeedUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/{memberId}/feed")
public class FeedController {

    private final GetMyFeedUseCase getMyFeedUseCase;

    public FeedController(GetMyFeedUseCase getMyFeedUseCase) {
        this.getMyFeedUseCase = getMyFeedUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getMyFeed(@PathVariable Long memberId) {
        List<FeedItemResponse> feed = getMyFeedUseCase.execute(memberId).stream()
                .map(FeedItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(feed));
    }
}
