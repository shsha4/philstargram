package com.study.philstargram;

import static org.assertj.core.api.Assertions.assertThat;

import com.study.philstargram.feed.application.FanOutFeedCommand;
import com.study.philstargram.feed.application.FanOutFeedUseCase;
import com.study.philstargram.follow.application.FollowMemberCommand;
import com.study.philstargram.follow.application.FollowMemberUseCase;
import com.study.philstargram.member.application.MemberResult;
import com.study.philstargram.member.application.SignUpMemberCommand;
import com.study.philstargram.member.application.SignUpMemberUseCase;
import com.study.philstargram.notification.application.NotifyNewPostCommand;
import com.study.philstargram.notification.application.NotifyNewPostUseCase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * phase 5b 컨슈머 idempotency: at-least-once 재전달을 모사한다. 같은 이벤트를 UseCase 에 두 번
 * 넘겨도(팬아웃/알림) 자연 유니크키 + ON CONFLICT DO NOTHING 덕분에 각각 한 건만 남는지 실제
 * Postgres 로 검증한다(UseCase → 어댑터 → native 쿼리 경로).
 */
class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SignUpMemberUseCase signUpMemberUseCase;

    @Autowired
    FollowMemberUseCase followMemberUseCase;

    @Autowired
    FanOutFeedUseCase fanOutFeedUseCase;

    @Autowired
    NotifyNewPostUseCase notifyNewPostUseCase;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 같은_이벤트를_두번_처리해도_피드와_알림은_각각_한건만_생성된다() {
        MemberResult author = signUpMemberUseCase.execute(new SignUpMemberCommand("idem-author@example.com", "author", null));
        MemberResult follower = signUpMemberUseCase.execute(new SignUpMemberCommand("idem-follower@example.com", "follower", null));
        followMemberUseCase.execute(new FollowMemberCommand(follower.id(), author.id()));

        long postId = 777_000L + author.id();
        LocalDateTime now = LocalDateTime.now();
        FanOutFeedCommand fanOut = new FanOutFeedCommand(postId, author.id(), "author", "hello", now);
        NotifyNewPostCommand notify = new NotifyNewPostCommand(postId, author.id(), "author", now);

        // 재전달 모사: 각 UseCase 를 두 번 호출
        fanOutFeedUseCase.execute(fanOut);
        fanOutFeedUseCase.execute(fanOut);
        notifyNewPostUseCase.execute(notify);
        notifyNewPostUseCase.execute(notify);

        Integer feedRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM feed_entries WHERE owner_member_id = ? AND post_id = ?",
                Integer.class, follower.id(), postId);
        Integer notiRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notifications WHERE dedup_key = ?",
                Integer.class, "NEW_POST:" + follower.id() + ":" + postId);

        assertThat(feedRows).isEqualTo(1);
        assertThat(notiRows).isEqualTo(1);
    }
}
