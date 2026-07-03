package com.study.philstargram.post.adapter.out.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * post 모듈이 소유하는 아웃바운드 토픽 선언. {@code post.created} 는 post 가 발행하는 공개 계약이며,
 * KafkaAdmin 이 앱 시작 시 이 토픽을 미리 생성한다.
 *
 * <p>미리 생성하는 이유: 컨슈머가 시작 시점에 <b>존재하지 않는</b> 토픽을 구독하면 새 토픽을
 * 인지하기까지 {@code metadata.max.age.ms}(기본 5분)만큼 지연될 수 있다. 토픽을 계약으로 못박아
 * 시작 시 만들어 두면 컨슈머가 곧바로 붙는다.
 *
 * <p>단일 브로커 로컬/테스트 환경이라 파티션 1, 복제 계수 1. 파티션 키는 작성자 id 이므로 파티션을
 * 늘려도 한 작성자의 게시글 순서는 보장된다.
 */
@Configuration
class PostEventTopicConfig {

    @Bean
    NewTopic postCreatedTopic() {
        return TopicBuilder.name("post.created").partitions(1).replicas(1).build();
    }
}
