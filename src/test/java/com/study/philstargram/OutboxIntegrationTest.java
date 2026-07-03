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
 * phase 4 이벤트 흐름 end-to-end: Outbox(이벤트 발행 레지스트리) → Kafka 외부화 → 컨슈머.
 *
 * <p>게시글을 만들면 PostCreatedEvent 발행이 event_publication 테이블에 기록되고, Modulith
 * externalizer 가 Kafka {@code post.created} 토픽으로 릴레이한 뒤 그 발행이 완료 처리
 * (completion_date 채워짐)된다. 이어서 feed/notification 의 Kafka 컨슈머가 각자 이벤트를 받아
 * 팔로워 피드와 알림을 채운다. 특히 피드/알림에 담긴 작성자 닉네임은 컨슈머가 member 를 조회한
 * 것이 아니라 <b>이벤트가 실어온 값(event-carried state)</b>임을 함께 검증한다.
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
    void postCreatedEventIsExternalizedToKafkaAndConsumedByFeedAndNotification() {
        MemberResult author = signUpMemberUseCase.execute(new SignUpMemberCommand("author@example.com", "author", null));
        MemberResult follower = signUpMemberUseCase.execute(new SignUpMemberCommand("follower@example.com", "follower", null));
        followMemberUseCase.execute(new FollowMemberCommand(follower.id(), author.id()));

        createPostUseCase.execute(new CreatePostCommand(author.id(), "outbox 테스트 게시글"));

        // 발행 즉시 event_publication 에 PostCreatedEvent 레코드가 쌓였는지 (같은 트랜잭션에서 기록)
        Integer published = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM event_publication WHERE event_type LIKE '%PostCreatedEvent'", Integer.class);
        assertThat(published).isGreaterThanOrEqualTo(1);

        // externalizer 가 Kafka 로 릴레이하면 모든 PostCreatedEvent 발행이 완료 처리된다.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Integer incomplete = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM event_publication "
                            + "WHERE event_type LIKE '%PostCreatedEvent' AND completion_date IS NULL",
                    Integer.class);
            assertThat(incomplete).isZero();
        });

        // feed 컨슈머가 Kafka 에서 받아 팔로워 피드를 채웠는지, 그리고 닉네임이 이벤트가 실어온 값인지.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Integer feedRows = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM feed_entries WHERE owner_member_id = ? AND author_nickname = 'author'",
                    Integer.class, follower.id());
            assertThat(feedRows).isEqualTo(1);
        });

        // notification 컨슈머가 같은 이벤트를 (독립 컨슈머 그룹으로) 받아 알림을 만들었는지.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Integer notiRows = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM notifications WHERE recipient_member_id = ? AND message LIKE 'author%'",
                    Integer.class, follower.id());
            assertThat(notiRows).isEqualTo(1);
        });
    }
}
