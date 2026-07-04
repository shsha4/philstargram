package com.study.philstargram.follow.adapter.out.stream;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * follower-count 집계 토폴로지(phase 5c). member.followed(+)/member.unfollowed(-) 이벤트를 받아
 * followee 별 팔로워 수를 KTable 로 유지하고, 상태 저장소 {@link #STORE_FOLLOWER_COUNTS} 로 노출한다.
 *
 * <p><b>멱등 모델링(핵심):</b> +1/-1 을 누적하지 않는다. 대신 <b>관계 키</b>({@code follower:followee})
 * 를 키로 하는 KTable 을 만든다 — 팔로우는 그 키에 값을 upsert, 언팔로우는 tombstone(null) 로 삭제.
 * 그 위에서 followee 로 groupBy 해 count 하므로, 같은 팔로우/언팔로우가 at-least-once 로 재전달돼도
 * 관계 키가 같아 카운트가 두 번 오르내리지 않는다(5b 자연 멱등성의 스트림 버전).
 *
 * <p>왜 Streams 인가(트레이드오프): 이 규모면 {@code SELECT count(*) FROM follows} 로도 충분하다.
 * Streams 를 쓰는 이유는 (a) 셀럽 판별 핫패스에서 DB 를 치지 않는 상시-최신 집계, (b) 상태 저장
 * 스트림 처리(KTable/interactive query)를 실제로 보여주는 학습 명분. 그 이상은 오버엔지니어링.
 */
@Configuration
@EnableKafkaStreams
class FollowerCountStreamConfig {

    static final String TOPIC_FOLLOWED = "member.followed";
    static final String TOPIC_UNFOLLOWED = "member.unfollowed";
    /** followee(String) → 팔로워 수(Long). interactive query 로 조회하는 상태 저장소 이름. */
    static final String STORE_FOLLOWER_COUNTS = "follower-counts";

    private final ObjectMapper objectMapper;

    FollowerCountStreamConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    KStream<String, String> followerCountTopology(StreamsBuilder builder) {
        // 값은 JSON 문자열(@KafkaListener 컨슈머와 동일한 계약) — 토폴로지에서 파싱한다.
        KStream<String, String> followed = builder.stream(TOPIC_FOLLOWED, Consumed.with(Serdes.String(), Serdes.String()));
        KStream<String, String> unfollowed = builder.stream(TOPIC_UNFOLLOWED, Consumed.with(Serdes.String(), Serdes.String()));

        // 관계 키(follower:followee) 기준으로 rekey. 팔로우=값 존재, 언팔로우=tombstone.
        KStream<String, String> relationships = followed.map((k, v) -> toRelationship(v, false))
                .merge(unfollowed.map((k, v) -> toRelationship(v, true)));

        // 관계 KTable: 키별 최신 상태(존재/삭제). 재전달돼도 같은 키 upsert 라 멱등.
        KTable<String, String> relationshipTable = relationships.toTable(
                Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("follow-relationships")
                        .withKeySerde(Serdes.String()).withValueSerde(Serdes.String()));

        // followee 로 묶어 count → tombstone 삭제 시 KGroupedTable 이 해당 그룹을 자동 감산한다.
        relationshipTable
                .groupBy((relKey, followee) -> KeyValue.pair(followee, followee), Grouped.with(Serdes.String(), Serdes.String()))
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(STORE_FOLLOWER_COUNTS)
                        .withKeySerde(Serdes.String()).withValueSerde(Serdes.Long()));

        return relationships;
    }

    private KeyValue<String, String> toRelationship(String json, boolean tombstone) {
        JsonNode node = objectMapper.readTree(json);
        long followerId = node.get("followerId").asLong();
        long followeeId = node.get("followeeId").asLong();
        String relKey = followerId + ":" + followeeId;
        return KeyValue.pair(relKey, tombstone ? null : String.valueOf(followeeId));
    }
}
