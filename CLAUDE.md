# CLAUDE.md

philstargram — SNS 백엔드를 **모듈러 모놀리스 + 헥사고날**로 설계하는 학습/면접용
프로젝트. 단순 CRUD 가 아니라 "필요하면 어떤 모듈이든 MSA 로 분리 가능한 구조"를
만드는 것이 목표다. 배경/설계 근거는 `README.md` 참고.

**언어 규칙: 모든 주석·Javadoc·문서·커밋 메시지는 한국어로 작성한다.** (코드/기술 용어는
영어 그대로 사용)

## 현재 상태

- **phase 5 완료** (Redis 피드 캐시 + 일관성 + 하이브리드 팬아웃). 다음은 phase 6(Elasticsearch/CQRS).
  - ✅ **5a Redis 피드 캐시**: `FeedCache` 아웃바운드 포트 + `RedisFeedCache` 어댑터(ZSET).
    진실의 원천=Postgres, Redis=버려도 되는 캐시. 읽기 cache-aside, 쓰기 write-through-if-present
    (append), 실패 시 fail-safe evict. 캐시 장애/이중 쓰기 유실은 TTL·미스 시점에 자가 치유
    (bounded staleness). 설계 근거는 `docs/learnings/redis-feed-cache-consistency.md`.
  - ✅ **5b 컨슈머 idempotency**: at-least-once 재전달 중복을 자연 멱등성(A)으로 흡수. feed 는
    `feed_entries UNIQUE(owner_member_id, post_id)`, notification 은 파생 dedup_key
    (`type:recipient:sourceId`) UNIQUE. 둘 다 native `ON CONFLICT DO NOTHING`(save() 는 충돌 시
    트랜잭션 abort 라 미사용). `NotifyNewPostCommand` 에 postId 추가. 근거는
    `docs/learnings/idempotency-and-delivery.md`.
  - ✅ **5c 하이브리드 팬아웃 + Kafka Streams**: 셀럽(팔로워 수 임계값 이상)은 쓰기 팬아웃 스킵 +
    팔로워 조회 시 셀럽 최근 글을 읽기 시점 pull 해 병합(`GetMyFeedUseCase`). 셀럽 판별용
    follower-count 는 `member.followed`/`member.unfollowed`(신규)를 **Kafka Streams KTable** 로
    집계 — **관계 키(`follower:followee`) upsert/tombstone** 으로 모델링해 재전달 멱등. 조회는
    interactive query(`FollowerCountView` 포트 → `follow.adapter.out.stream`). 읽기 pull 이
    feed→member 결합을 만들지 않도록 `posts.author_nickname` 스냅샷 추가(feed→post 만).
    - 경계: Streams 토폴로지는 `follow` 어댑터, 셀럽 판별은 `follow.application`. feed→post 는
      `post.application`(PostQueryService) 경유. 새 모듈 없음.
- **phase 4 완료** (Kafka 외부화 + 결합 제거). 아래 phase 3 위에서.
  - ✅ 이벤트 외부화: `PostCreatedEvent`/`MemberFollowedEvent` 에 `@Externalized("topic::#{key}")`
    → 기존 JDBC Outbox 레지스트리가 Kafka 토픽(`post.created`/`member.followed`)으로 릴레이.
  - ✅ 컨슈머 전환: feed/notification 의 `adapter.in.event` 를 `@ApplicationModuleListener`
    → `@KafkaListener`(독립 컨슈머 그룹 `feed`/`notification`)로 교체. 어댑터→UseCase 구조 유지.
  - ✅ event-carried state: 이벤트에 작성자/팔로워 nickname 을 실어 feed/notification 의
    `MemberQueryService` 동기 호출 제거 → **feed·notification 이 더 이상 member 에 의존하지 않음**.
  - ✅ 토픽 = 공개 계약: 각 생산 모듈이 `NewTopic` 빈으로 토픽을 시작 시 선언(컨슈머 토픽 발견 지연 방지).
- **phase 3 완료** (v1 MVP + Spring Modulith 이벤트까지 완료된 위에서).
  - ✅ Outbox (Spring Modulith JDBC 이벤트 발행 레지스트리)
  - ✅ 게시글 길이 제한 중복 규칙 제거 (도메인 단일 소스 + `IllegalArgumentException`→400)
  - ✅ 이벤트 리스너를 `adapter.in.event` + UseCase 로 분리
  - ✅ 값 객체 `Email`/`Nickname` (도메인이 형식/불변식 보장, web DTO 의 `@Email`/`@Size` 제거)
  - ✅ 타입드 ID `MemberId`/`PostId`/`FollowId` (모듈 내부 식별자만 타입드, 모듈 경계·이벤트는 raw `Long`)
  - ✅ 도메인 이벤트를 애그리거트가 raise (`PostWritten`/`MemberFollowed` 누적 → UseCase 가 드레인·번역·발행)
- 로드맵은 `README.md` 의 "로드맵" 절이 기준이며, **인프라 단계마다 도메인 개선을
  인터리빙**하는 개정판을 따른다.
- 모듈: `member`, `post`, `follow`, `feed`, `notification`, `common`
  (루트 패키지 `com.study.philstargram`).

## 절대 지켜야 할 경계 규칙 (위반 시 빌드 실패)

1. **모듈 간 내부 참조 금지.** 다른 모듈은 각 모듈의 `application` 패키지
   (`*QueryService`/`*UseCase`/이벤트)만 의존한다. `domain`·`adapter` 는 내부 구현.
2. **`domain` 은 프레임워크를 모른다.** `..domain..` 에 JPA/Spring/Hibernate/Jackson
   타입 금지. 도메인은 자기 규칙만 표현.
3. **모듈 간 DB 조인 금지.** 각 모듈이 자기 테이블 소유. 다른 모듈 id 는 단순 컬럼
   (외래 키/`@ManyToOne` 아님).
4. **상태 변경은 UseCase 를 통해서만.** 컨트롤러가 엔티티 직접 조작 금지.
5. **모듈 간 순환 의존 금지.**
6. **모듈 간 협력은 인프로세스 메서드 호출(application API) 또는 이벤트로.** 자기 앱의
   REST 컨트롤러를 내부에서 호출하지 않는다.

이 규칙들은 `ArchitectureTest`(ArchUnit) + `ModularityTest`(Spring Modulith
`verify()`) 로 강제된다. **코드 변경 후 반드시 이 둘을 통과시켜라.**

## 패키지/코드 컨벤션

- 모듈별 헥사고날 레이어: `adapter.in.web` → `application` → `domain`,
  `adapter.out.persistence` → `application`/`domain`.
- DTO 3계층 분리 유지: web(`*Request`/`*Response`) / application(`*Command`/`*Result`,
  `*Summary`) / domain. 섞지 말 것.
- 어댑터 구현체·`*JpaRepository` 는 **package-private** 로 캡슐화.
- 도메인 객체는 `signUp`/`write`/`create` 같은 정적 팩토리 + `reconstitute`(영속성
  복원용) 패턴을 따른다.
- 모듈을 새로 만들면: 모듈 루트에 `package-info.java`(모듈 설명), `application` 에
  `@NamedInterface("application")` 붙인 `package-info.java` 를 추가해야 Modulith
  `verify()` 가 통과한다. `common` 은 `@ApplicationModule(type = OPEN)`.

## 빌드 / 테스트 (Windows, git bash)

```bash
./gradlew.bat build          # 컴파일 + 전체 테스트
./gradlew.bat test           # 테스트만

# 전체 스택(운영 형태): postgres + backend + frontend 를 한 번에
docker compose up -d --build # frontend http://localhost:3000, backend :8080, db :5432

# 개발(핫리로드): DB 만 컨테이너로 띄우고 앱은 로컬 실행
docker compose up -d postgres  # PostgreSQL 만 (localhost:5432)
./gradlew.bat bootRun          # 백엔드 (localhost:8080, schema.sql 자동 실행)
# front/ 에서: npm run dev      # Vite (localhost:5173, /api 는 8080 으로 프록시)
```

- 프론트는 `/api` 를 **상대 경로**로 호출한다 → 개발은 Vite 프록시, 운영은 nginx 프록시가
  백엔드로 넘긴다(같은 오리진이라 CORS 불필요). 백엔드는 컨테이너에서 `SPRING_DATASOURCE_URL`
  env 로 `postgres` 호스트를 가리키고, 로컬 실행 시엔 application.yaml 의 localhost 기본값을 쓴다.

- 테스트는 `docker compose up` 불필요 — Testcontainers 가 각자 PostgreSQL 을 띄운다.
  단 **Docker 데몬은 실행 중이어야** 통합 테스트가 돈다.
- 통합 테스트는 H2 가 아니라 Testcontainers PostgreSQL 을 쓴다(`@DataJpaTest` +
  `@ServiceConnection`).
- 테스트 전략: UseCase 단위 테스트(Mockito, 포트 목킹) + 리포지토리 통합 테스트
  (Testcontainers) + 경계 테스트(ArchUnit/Modulith).

## 스택 주의사항 (Spring Boot 4.1 / JDK 21)

- Spring Boot **4.1.0** + Spring Framework 7. Boot 3.x 대비 test-autoconfigure 패키지가
  이동함: `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`,
  `@AutoConfigureTestDatabase` → `org.springframework.boot.jdbc.test.autoconfigure`.
  `spring-boot-starter-data-jpa-test` 의존성 별도 필요(스타터-test 에 미포함).
- Testcontainers **2.0.5**: 아티팩트명이 `testcontainers-junit-jupiter` /
  `testcontainers-postgresql` 로 변경됨(구 `junit-jupiter`/`postgresql` 아님).
  `PostgreSQLContainer` 는 non-generic → `new PostgreSQLContainer("postgres:16-alpine")`
  (`<>` 없이), import 는 `org.testcontainers.postgresql`.
- Spring Modulith **2.1.0**. `@ApplicationModuleListener` 는 `spring-modulith-events-api`
  에 있음(BOM import 필요).
- **Kafka (phase 4)**: `spring-kafka`(Boot 4.1 이 4.1.0 으로 버전 관리) + `spring-modulith-events-kafka`
  (BOM). 이벤트 외부화는 `@Externalized("topic::#{spel}")`.
- **Jackson 3 주의**: Boot 4.1/Spring 7 은 Jackson 3 가 기본(`tools.jackson.*`, Jackson 2
  `com.fasterxml.jackson.databind` 는 클래스패스에 없음). spring-kafka 의 `JsonSerializer`/
  `JsonDeserializer` 는 deprecated → Jackson 3 는 `JacksonJson*`.
- **Kafka 직렬화 함정(경험)**: Modulith externalizer 는 이벤트를 **이미 JSON byte[]** 로 직렬화해
  KafkaTemplate 에 넘긴다. 따라서 producer `value-serializer` 는 반드시 **`ByteArraySerializer`**
  (그대로 통과). `JsonSerializer` 를 쓰면 byte[] 를 다시 직렬화해 **base64** 가 된다. 컨슈머는
  `value-deserializer: StringDeserializer` + `@KafkaListener` 파라미터를 이벤트 타입으로 두면
  Boot 가 자동 등록한 Jackson 메시지 컨버터가 JSON→이벤트로 역직렬화한다.
- **Testcontainers Kafka**: `org.testcontainers.kafka.KafkaContainer`(apache/kafka 이미지)는
  `apache/kafka:3.9.0` 의 StorageTool 포맷 검증과 충돌(`advertised.listeners=0.0.0.0`)해 기동 실패.
  테스트는 `ConfluentKafkaContainer`(`confluentinc/cp-kafka`)로 띄운다. 운영 docker-compose 는
  apache/kafka 그대로(거긴 advertised 가 routable 이라 정상).
- 버전/좌표가 바뀐 것 같으면 웹으로 재확인 후 진행.

## 작업 원칙

- **넓이보다 깊이.** phase 4~7 은 "다 구현"보다 "왜/트레이드오프를 설명할 수 있는 깊이"를
  우선. resume-driven 기술 나열 금지.
- **오버엔지니어링 금지.** SNS 도메인은 본질적으로 CRUD 에 가까움. 전술 DDD 개선은
  "도메인이 자기 규칙을 갖는다"를 보여주는 수준까지만, 억지 추상화 금지.
- **로드맵 순서 준수.** 앞 단계(현재 phase 4 완료) 전에 Redis/ES 로 건너뛰지 말 것. 다음은 phase 5.
- 큰 기능은 새 세션에서 시작하고, 완료 단위로 커밋. 브랜치는 `main`, remote `origin` 설정됨.

## phase 3 전술 DDD 개선 (완료) — 설계 결정 기록

아키텍처 리뷰 결과 헥사고날/경계는 강하나 전술 DDD 가 약했던(빈혈 도메인) 점을 phase 3 에서
해소했다. 결정 근거를 남긴다:

- **값 객체 `Email`/`Nickname`**: 형식·길이 불변식을 도메인이 스스로 보장한다(생성자 검증).
  web DTO 의 `@Email`/`@Size(nickname)` 은 제거 — 게시글 길이 규칙과 동일하게 "도메인이
  규칙을 갖고, web 중복 검증 없이 `IllegalArgumentException`→400" 원칙을 따른다. `bio` 는
  도메인 불변식이 없는 선택 필드라 web `@Size(max=500)` 만 유지. Result/Summary/영속성 매핑은
  `.value()` 로 String 유지.
- **타입드 ID `MemberId`/`PostId`/`FollowId`**: 각 애그리거트의 **자기 식별자와 아웃바운드
  리포지토리 포트만** 타입드. **모듈 경계와 이벤트는 raw `Long`** 유지(post 의 `authorId`,
  follow 의 `followerId/followeeId`, `MemberSummary`, `*Event`) → 모듈 간 타입 결합 회피.
  모듈 공개 API(UseCase/QueryService)는 `Long` 을 받고 경계에서 `XxxId.of(...)` 로 감싼다.
  JPA `@Id` 는 `Long` 그대로, 매핑 시 `.value()`/`XxxId.of()` 로 변환.
- **도메인 이벤트를 애그리거트가 raise**: 애그리거트가 내부에 이벤트를 누적하고
  `pullDomainEvents()` 로 드레인하는 자체 패턴(Spring `AbstractAggregateRoot` 미사용, JPA 결합
  회피). DB 가 식별자를 생성하므로, 저장 후 어댑터가 `assignId()` 로 식별자를 되돌려 부여하고
  그 시점의 상태로 이벤트를 만든다(같은 인스턴스 반환). UseCase 가 도메인 이벤트
  (`PostWritten`/`MemberFollowed`, 모듈 내부)를 모듈 간 계약(`PostCreatedEvent`/
  `MemberFollowedEvent`, application)으로 번역해 발행한다.

## phase 4 Kafka + 결합 제거 (완료) — 설계 결정 기록

phase 3 의 Outbox 를 이어받아, 인프로세스 이벤트를 Kafka 외부 메시지로 확장하고 컨슈머를 독립
배포 가능하게 만들었다. 결정 근거:

- **분리 수준 = (a) 모놀리스 유지 + 전송만 Kafka**. 실제 서비스 물리 분리는 로드맵상 phase 7 목표라
  이번엔 "분리 가능한 상태"까지만. feed 를 별도 앱으로 뽑는 (b) 안은 phase 7 로 미룸.
- **Kafka Streams 미도입**: feed/notification 은 "이벤트 → DB 사이드이펙트"라 전형적 컨슈머
  (`@KafkaListener`)가 헥사고날 인입 어댑터 모양에 정확히 맞는다. Streams(조인/집계/상태)의 명분이
  없어 phase 4 에선 오버엔지니어링. Streams 는 **phase 5 하이브리드 팬아웃의 follower-count 집계**
  라는 실제 요구가 생길 때 검토.
- **event-carried state**: 작성자/팔로워 nickname 을 이벤트 페이로드에 실어, N 개의 컨슈머가 각자
  member 를 동기 조회하던 것을 없앴다. 생산자(post/follow)가 발행 시점에 이미 member 에 의존하므로
  새 결합이 생기지 않고, 오히려 **feed·notification → member 의존이 사라진다**. 기존 피드 조회
  비정규화(닉네임 스냅샷)와 같은 결의 결정.
- **externalization 방식**: `@Externalized("post.created::#{#this.authorId().toString()}")` — 토픽명은
  공개 계약, 파티션 키는 작성자/팔로위 id(작성자별·수신자별 순서 보장). 기존 JDBC 이벤트 발행
  레지스트리가 그대로 Kafka 릴레이(Outbox)가 되고, `republish-outstanding-events-on-restart` 로
  재시작 시 미발행 건을 재발행.
- **계약 공유 범위**: 모놀리스라 컨슈머가 생산자의 `*.application` 이벤트 레코드를 그대로 역직렬화
  (application 은 공개 API 라 경계 규칙 위반 아님). 실제 분리(phase 7) 시 컨슈머가 자체 계약 레코드를
  두면 됨. 타입 헤더(생산자 FQN)에 의존하지 않아 JSON 형태만이 계약이다.
- **직렬화**: producer `value-serializer=ByteArraySerializer`(Modulith 가 만든 JSON byte[] 통과),
  consumer 는 `StringDeserializer` + 타입드 `@KafkaListener` 파라미터(Jackson 메시지 컨버터가
  역직렬화). 배경/함정은 위 "스택 주의사항" 참고.
- **트레이드오프(미해결, 의도적)**: 컨슈머는 at-least-once — 재전달 시 팬아웃/알림 중복 가능.
  idempotency 는 phase 5(Redis/일관성)에서 다룬다. 지금 넣으면 범위 초과.
- **검증**: `OutboxIntegrationTest` 가 Testcontainers 실제 브로커로 post 작성 → Kafka 릴레이 →
  feed/notification 컨슈머 소비까지 end-to-end 확인(피드/알림의 닉네임이 이벤트가 실어온 값인지도 검증).
