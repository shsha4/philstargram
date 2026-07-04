package com.study.philstargram.follow.adapter.out.stream;

import com.study.philstargram.follow.application.FollowerCountView;
import java.util.Optional;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

/**
 * {@link FollowerCountView} 의 Kafka Streams 구현(phase 5c). {@link FollowerCountStreamConfig} 가
 * 유지하는 상태 저장소를 interactive query 로 읽는다.
 *
 * <p>스토어가 아직 준비되지 않았거나(기동/리밸런스) 스트림이 RUNNING 이 아니면 {@link Optional#empty()}
 * 를 반환한다 → 호출측은 "셀럽 아님"으로 안전하게 처리(결과적 일관성 하에서 정답성 우선).
 */
@Component
class KafkaStreamsFollowerCountView implements FollowerCountView {

    private final StreamsBuilderFactoryBean streamsFactory;

    KafkaStreamsFollowerCountView(StreamsBuilderFactoryBean streamsFactory) {
        this.streamsFactory = streamsFactory;
    }

    @Override
    public Optional<Long> countFor(Long memberId) {
        KafkaStreams streams = streamsFactory.getKafkaStreams();
        if (streams == null || streams.state() != KafkaStreams.State.RUNNING) {
            return Optional.empty();
        }
        try {
            ReadOnlyKeyValueStore<String, Long> store = streams.store(
                    StoreQueryParameters.fromNameAndType(
                            FollowerCountStreamConfig.STORE_FOLLOWER_COUNTS, QueryableStoreTypes.keyValueStore()));
            return Optional.ofNullable(store.get(String.valueOf(memberId)));
        } catch (InvalidStateStoreException e) {
            return Optional.empty(); // 스토어 재구성 중
        }
    }
}
