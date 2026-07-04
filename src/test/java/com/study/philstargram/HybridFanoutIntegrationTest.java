package com.study.philstargram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.study.philstargram.feed.application.FeedItem;
import com.study.philstargram.feed.application.GetMyFeedUseCase;
import com.study.philstargram.follow.application.FollowMemberCommand;
import com.study.philstargram.follow.application.FollowMemberUseCase;
import com.study.philstargram.follow.application.FollowQueryService;
import com.study.philstargram.member.application.MemberResult;
import com.study.philstargram.member.application.SignUpMemberCommand;
import com.study.philstargram.member.application.SignUpMemberUseCase;
import com.study.philstargram.post.application.CreatePostCommand;
import com.study.philstargram.post.application.CreatePostUseCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * phase 5c 하이브리드 팬아웃 end-to-end. 셀럽 판별을 위해 임계값을 2 로 낮추고, follower-count
 * 집계(Kafka Streams)가 실제 브로커를 통해 동작하는 것을 확인한다.
 *
 * <p>흐름: 셀럽 C 를 두 명이 팔로우 → Streams 가 집계해 isCeleb(C)=true → C 가 글을 쓰면 쓰기
 * 팬아웃이 <b>건너뛰어져</b> 팔로워 피드 테이블에 적재되지 않지만, 팔로워의 피드 조회 시 셀럽 글이
 * <b>읽기 시점 pull</b> 로 병합돼 보인다.
 *
 * <p>기본 통합 테스트 컨텍스트(임계값 1000)와 충돌하지 않도록 <b>별도 Streams application-id</b> 와
 * 매 실행 새 state 디렉터리를 써 독립된 컨텍스트로 띄운다.
 */
@TestPropertySource(properties = {
        "philstargram.follow.celeb-threshold=2",
        "spring.kafka.streams.application-id=hybrid-fanout-test"
})
class HybridFanoutIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void streamsStateDir(DynamicPropertyRegistry registry) throws IOException {
        Path dir = Files.createTempDirectory("hybrid-streams-state");
        registry.add("spring.kafka.streams.properties.state.dir", dir::toString);
    }

    @Autowired
    SignUpMemberUseCase signUpMemberUseCase;

    @Autowired
    FollowMemberUseCase followMemberUseCase;

    @Autowired
    CreatePostUseCase createPostUseCase;

    @Autowired
    GetMyFeedUseCase getMyFeedUseCase;

    @Autowired
    FollowQueryService followQueryService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 셀럽_글은_쓰기_팬아웃되지_않고_읽기시점에_pull_되어_피드에_병합된다() {
        MemberResult celeb = signUpMemberUseCase.execute(new SignUpMemberCommand("celeb@example.com", "celeb", null));
        MemberResult reader = signUpMemberUseCase.execute(new SignUpMemberCommand("reader@example.com", "reader", null));
        MemberResult other = signUpMemberUseCase.execute(new SignUpMemberCommand("other@example.com", "other", null));

        // 셀럽을 두 명이 팔로우 → 임계값(2) 도달. member.followed 가 Kafka 로 나가 Streams 가 집계한다.
        followMemberUseCase.execute(new FollowMemberCommand(reader.id(), celeb.id()));
        followMemberUseCase.execute(new FollowMemberCommand(other.id(), celeb.id()));

        // Streams 가 집계를 끝내 셀럽으로 판별될 때까지 대기(외부화→릴레이→집계 end-to-end).
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(followQueryService.isCeleb(celeb.id())).isTrue());

        // 셀럽이 글 작성 → 쓰기 팬아웃 스킵 대상.
        createPostUseCase.execute(new CreatePostCommand(celeb.id(), "셀럽의 게시글"));

        // 읽기 시점 pull: reader 의 피드에 셀럽 글이 병합돼 보인다.
        List<FeedItem> feed = getMyFeedUseCase.execute(reader.id());
        assertThat(feed).extracting(FeedItem::authorNickname).contains("celeb");
        assertThat(feed).extracting(FeedItem::contentPreview).contains("셀럽의 게시글");

        // 쓰기 팬아웃은 건너뛰어졌으므로 reader 의 피드 테이블에는 셀럽 글이 적재되지 않는다.
        Integer materializedRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM feed_entries WHERE owner_member_id = ? AND author_id = ?",
                Integer.class, reader.id(), celeb.id());
        assertThat(materializedRows).isZero();
    }
}
