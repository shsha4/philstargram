package com.study.philstargram;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 전체 컨텍스트(@SpringBootTest) 통합 테스트의 공통 베이스.
 *
 * <p>PostgreSQL 컨테이너를 {@code @Container} 없이 <b>싱글턴</b>으로 한 번만 수동 기동한다.
 * 이유: {@code @Container} 로 띄우면 JUnit 이 클래스 종료 시 컨테이너를 멈추는데, 캐시된
 * 스프링 컨텍스트는 JVM 종료 훅에서 뒤늦게 닫히면서 Modulith 이벤트 레지스트리 종료 훅이
 * 이미 죽은 DB 로 커넥션을 시도해 30초 타임아웃을 낸다. 수동 기동한 컨테이너는 Testcontainers
 * 의 Ryuk 이 JVM 종료 시 정리하므로 컨텍스트가 닫힐 때까지 살아있어 이 문제가 없고, 여러
 * 통합 테스트가 컨테이너와 컨텍스트를 공유해 더 빠르다.
 */
@SpringBootTest
abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
