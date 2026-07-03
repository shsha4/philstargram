package com.study.philstargram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.study.philstargram.follow.application.FollowMemberCommand;
import com.study.philstargram.follow.application.FollowMemberUseCase;
import com.study.philstargram.member.application.MemberResult;
import com.study.philstargram.member.application.SignUpMemberCommand;
import com.study.philstargram.member.application.SignUpMemberUseCase;
import com.study.philstargram.post.application.CreatePostCommand;
import com.study.philstargram.post.application.CreatePostUseCase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Outbox(Spring Modulith 이벤트 발행 레지스트리)가 실제로 동작하는지 검증한다:
 * 게시글을 만들면 PostCreatedEvent 발행이 event_publication 테이블에 기록되고,
 * 비동기 리스너들이 끝난 뒤 완료 처리(completion_date 채워짐)되며, 그 결과
 * 팔로워의 피드가 실제로 채워진다.
 */
class OutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SignUpMemberUseCase signUpMemberUseCase;

    @Autowired
    FollowMemberUseCase followMemberUseCase;

    @Autowired
    CreatePostUseCase createPostUseCase;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void postCreatedEventIsPersistedToOutboxAndCompletedAfterListenersRun() {
        MemberResult author = signUpMemberUseCase.execute(new SignUpMemberCommand("author@example.com", "author", null));
        MemberResult follower = signUpMemberUseCase.execute(new SignUpMemberCommand("follower@example.com", "follower", null));
        followMemberUseCase.execute(new FollowMemberCommand(follower.id(), author.id()));

        createPostUseCase.execute(new CreatePostCommand(author.id(), "outbox 테스트 게시글"));

        // 발행 즉시 event_publication 에 PostCreatedEvent 레코드가 쌓였는지 (같은 트랜잭션에서 기록)
        Integer published = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM event_publication WHERE event_type LIKE '%PostCreatedEvent'", Integer.class);
        assertThat(published).isGreaterThanOrEqualTo(1);

        // 비동기 리스너가 모두 끝나면 모든 PostCreatedEvent 발행이 완료 처리된다.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer incomplete = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM event_publication "
                            + "WHERE event_type LIKE '%PostCreatedEvent' AND completion_date IS NULL",
                    Integer.class);
            assertThat(incomplete).isZero();
        });

        // 리스너가 실제로 동작해 팔로워 피드가 채워졌는지 (end-to-end)
        Integer feedRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM feed_entries WHERE owner_member_id = ?", Integer.class, follower.id());
        assertThat(feedRows).isEqualTo(1);
    }
}
