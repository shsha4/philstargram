package com.study.philstargram.follow.adapter.out.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * follow 모듈이 소유하는 아웃바운드 토픽 선언. {@code member.followed} 는 follow 가 발행하는 공개
 * 계약이며, KafkaAdmin 이 앱 시작 시 미리 생성한다(컨슈머의 토픽 발견 지연 방지 — 자세한 배경은
 * post 쪽 {@code PostEventTopicConfig} 참고).
 *
 * <p>단일 브로커 로컬/테스트 환경이라 파티션 1, 복제 계수 1.
 */
@Configuration
class FollowEventTopicConfig {

    @Bean
    NewTopic memberFollowedTopic() {
        return TopicBuilder.name("member.followed").partitions(1).replicas(1).build();
    }
}
