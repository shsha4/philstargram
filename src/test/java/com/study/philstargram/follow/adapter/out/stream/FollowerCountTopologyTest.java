package com.study.philstargram.follow.adapter.out.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * follower-count 토폴로지(phase 5c)를 {@link TopologyTestDriver} 로 브로커 없이 결정적으로 검증한다.
 * 핵심은 <b>관계 키 기반 upsert/tombstone</b> 모델이 (a) 정확히 집계하고 (b) 재전달에 멱등하며
 * (c) 언팔로우 시 감산하는지다.
 */
class FollowerCountTopologyTest {

    private TopologyTestDriver driver;
    private TestInputTopic<String, String> followed;
    private TestInputTopic<String, String> unfollowed;
    private KeyValueStore<String, Long> counts;

    @BeforeEach
    void setUp() {
        StreamsBuilder builder = new StreamsBuilder();
        new FollowerCountStreamConfig(JsonMapper.builder().build()).followerCountTopology(builder);
        Topology topology = builder.build();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "follower-count-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        driver = new TopologyTestDriver(topology, props);
        followed = driver.createInputTopic(FollowerCountStreamConfig.TOPIC_FOLLOWED, new StringSerializer(), new StringSerializer());
        unfollowed = driver.createInputTopic(FollowerCountStreamConfig.TOPIC_UNFOLLOWED, new StringSerializer(), new StringSerializer());
        counts = driver.getKeyValueStore(FollowerCountStreamConfig.STORE_FOLLOWER_COUNTS);
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    @Test
    void 팔로우가_쌓이면_followee_별_팔로워수를_집계한다() {
        follow(10, 100);
        follow(20, 100);
        follow(30, 200);

        assertThat(counts.get("100")).isEqualTo(2L);
        assertThat(counts.get("200")).isEqualTo(1L);
    }

    @Test
    void 같은_팔로우가_재전달돼도_카운트가_두번_오르지_않는다() {
        follow(10, 100);
        follow(10, 100); // at-least-once 재전달 — 관계 키(10:100)가 같아 멱등

        assertThat(counts.get("100")).isEqualTo(1L);
    }

    @Test
    void 언팔로우하면_카운트가_감산된다() {
        follow(10, 100);
        follow(20, 100);
        assertThat(counts.get("100")).isEqualTo(2L);

        unfollow(10, 100); // tombstone → 관계 삭제 → 그룹 카운트 -1

        assertThat(counts.get("100")).isEqualTo(1L);
    }

    private void follow(long followerId, long followeeId) {
        followed.pipeInput(String.valueOf(followeeId), json(followerId, followeeId));
    }

    private void unfollow(long followerId, long followeeId) {
        unfollowed.pipeInput(String.valueOf(followeeId), json(followerId, followeeId));
    }

    private static String json(long followerId, long followeeId) {
        return "{\"followerId\":" + followerId + ",\"followeeId\":" + followeeId + "}";
    }
}
