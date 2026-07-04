package com.study.philstargram;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 전체 컨텍스트(@SpringBootTest) 통합 테스트의 공통 베이스.
 *
 * <p>PostgreSQL 과 Kafka(KRaft) 컨테이너를 {@code @Container} 없이 <b>싱글턴</b>으로 한 번만
 * 수동 기동한다. 이유: {@code @Container} 로 띄우면 JUnit 이 클래스 종료 시 컨테이너를 멈추는데,
 * 캐시된 스프링 컨텍스트는 JVM 종료 훅에서 뒤늦게 닫히면서 Modulith 이벤트 레지스트리 종료 훅이
 * 이미 죽은 DB 로 커넥션을 시도해 30초 타임아웃을 낸다. 수동 기동한 컨테이너는 Testcontainers
 * 의 Ryuk 이 JVM 종료 시 정리하므로 컨텍스트가 닫힐 때까지 살아있어 이 문제가 없고, 여러
 * 통합 테스트가 컨테이너와 컨텍스트를 공유해 더 빠르다.
 *
 * <p>{@code @ServiceConnection} 이 각 컨테이너의 접속 정보를 스프링 프로퍼티
 * (datasource, spring.kafka.bootstrap-servers)로 자동 주입한다. phase 4 부터 이벤트 전송이
 * Kafka 라, 이벤트 흐름을 검증하는 통합 테스트는 실제 브로커가 필요하다.
 */
@SpringBootTest
abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    // Testcontainers 의 apache/kafka용 KafkaContainer 는 apache/kafka:3.9.0 의 StorageTool
    // 포맷 단계와 충돌(advertised.listeners=0.0.0.0)해 기동이 실패한다. @ServiceConnection 이
    // 동일하게 지원하는 Confluent 이미지 컨테이너로 띄운다(운영 docker-compose 는 apache/kafka 사용).
    @ServiceConnection
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    // 피드 읽기 캐시(phase 5a). GenericContainer 라 @ServiceConnection 에 name="redis" 힌트를 줘야
    // Boot 의 Redis ConnectionDetailsFactory 가 매칭해 spring.data.redis.* 프로퍼티를 자동 주입한다.
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        KAFKA.start();
        REDIS.start();
    }
}
