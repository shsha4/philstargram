package com.study.philstargram.follow.adapter.in.web;

import com.study.philstargram.follow.application.FollowMemberCommand;
import com.study.philstargram.follow.application.FollowMemberUseCase;
import com.study.philstargram.follow.application.UnfollowMemberCommand;
import com.study.philstargram.follow.application.UnfollowMemberUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/{followerId}/follow")
public class FollowController {

    private final FollowMemberUseCase followMemberUseCase;
    private final UnfollowMemberUseCase unfollowMemberUseCase;

    public FollowController(FollowMemberUseCase followMemberUseCase, UnfollowMemberUseCase unfollowMemberUseCase) {
        this.followMemberUseCase = followMemberUseCase;
        this.unfollowMemberUseCase = unfollowMemberUseCase;
    }

    @PostMapping("/{followeeId}")
    public ResponseEntity<Void> follow(@PathVariable Long followerId, @PathVariable Long followeeId) {
        followMemberUseCase.execute(new FollowMemberCommand(followerId, followeeId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{followeeId}")
    public ResponseEntity<Void> unfollow(@PathVariable Long followerId, @PathVariable Long followeeId) {
        unfollowMemberUseCase.execute(new UnfollowMemberCommand(followerId, followeeId));
        return ResponseEntity.noContent().build();
    }
}
