# philstargram — SNS 모듈러 모놀리스

백엔드 학습용 프로젝트입니다. 단순 CRUD SNS 가 아니라, 하나의 Spring Boot
애플리케이션 안에서 모듈 경계를 충분히 엄격하게 유지하여 **필요하면 어떤 모듈이든
별도 서비스로 분리(MSA)** 할 수 있도록 만드는 연습입니다.

## 기술 스택

Java 21, Spring Boot 4.1, Spring Modulith(+ 이벤트 외부화), Spring Data JPA, PostgreSQL,
Kafka(Spring Kafka), Testcontainers, ArchUnit. Redis / Elasticsearch 는 아래 로드맵에
따라 한꺼번에가 아니라 단계적으로 도입합니다.

## 모듈 구성과 분리 이유

```
com.study.philstargram
├── common          공통 예외, ApiResponse 응답 래퍼, 전역 예외 핸들러
├── member          회원 신원(identity) & 프로필
├── post            게시글 생명주기 (생성/조회)
├── follow          팔로우 그래프
├── feed            사용자별 타임라인 읽기 모델 (쓰기 시점 팬아웃)
└── notification    이벤트 기반 알림
```

- **member** 는 회원의 신원과 프로필 데이터를 소유합니다. 다른 모듈은 이 테이블을
  건드리지 않습니다.
- **follow** 는 의도적으로 `member` 안에 두지 않았습니다. 팔로우 관계는 프로필
  데이터(신원/설정)와는 변경되는 이유가 완전히 다른 그래프 간선이며, 읽기/쓰기
  패턴(팬아웃 중심)도 크게 다릅니다.
- **post** 는 게시글 생명주기를 소유하며, 누가 누구를 팔로우하는지는 알지 못합니다.
- **feed** 는 게시글 저장소가 **아니라** 사용자 타임라인 읽기 모델입니다.
  `post` 의 `PostCreatedEvent` 에 반응하여 팔로워들의 피드에 미리 적재합니다
  (아래 "피드 전략" 참고).
- **notification** 은 다른 모듈의 이벤트에 반응해 알림을 만드는 독립 모듈입니다.
  자체 생명주기는 없습니다.

### 핵심 경계 규칙

1. **모듈 간 내부 구현 직접 참조 금지.** 각 모듈은 `application` 패키지
   (`*QueryService`, `*UseCase`, 이벤트)만이 다른 모듈을 위한 공개 API 입니다.
   `domain` 과 `adapter` 패키지는 다른 모듈에서 접근할 수 없습니다.
   `ArchitectureTest`(`modulesDoNotReachIntoEachOthersInternalPackages`)가 강제합니다.
2. **`domain` 은 기술을 모른다.** `..domain..` 패키지에는 JPA, Spring, Hibernate,
   Jackson 타입이 없습니다. 도메인은 자기 규칙만 표현합니다.
   `ArchitectureTest`(`domainPackagesStayFrameworkFree`)가 강제합니다.
3. **모듈 간 DB 조인 금지.** 각 모듈은 자기 테이블을 소유합니다
   (`members`, `posts`, `follows`, `feed_entries`, `notifications`).
   `author_id`/`follower_id`/`recipient_member_id` 등은 단순 `BIGINT` 컬럼일 뿐,
   모듈 경계를 넘는 JPA `@ManyToOne` 연관관계가 아닙니다. 지금은 물리적으로 하나의
   DB 를 쓰지만, 나중에 분리하는 것을 막지 않습니다.
4. **상태 변경은 UseCase 를 통해서만.** 컨트롤러는 엔티티를 직접 조작하지 않고,
   단일 목적 `*UseCase`(`SignUpMemberUseCase`, `CreatePostUseCase`,
   `FollowMemberUseCase` 등)를 호출합니다.
5. **모듈 간 순환 의존 금지.** `ArchitectureTest`(`modulesAreFreeOfCycles`)가
   강제합니다. 현재 의존 그래프는 다음과 같습니다:
   `post`, `follow` → `member`; `feed`, `notification` → `follow`.
   (phase 4 의 event-carried state 로 `feed`/`notification` 의 `member` 의존이 사라졌습니다 —
   필요한 작성자/팔로워 닉네임을 이벤트가 실어오기 때문입니다.)

모듈별 헥사고날 흐름: `adapter.in.web → application → domain`,
`adapter.out.persistence → application/domain`, 그리고 `domain` 은 바깥을 향하지
않습니다.

경계 규칙은 손으로 작성한 `ArchitectureTest`(ArchUnit)와 Spring Modulith 의
`ApplicationModules.verify()`(`ModularityTest`) 두 곳에서 교차 검증됩니다.

## 피드 전략

기본 전략은 **쓰기 시점 팬아웃(fan-out-on-write)** 입니다. SNS 에서는 피드 조회가
쓰기보다 훨씬 잦으므로, 게시글 작성 시점에 팔로워들의 피드에 미리 적재하여 읽기
성능을 우선합니다.

동작 방식: `CreatePostUseCase` 가 `PostCreatedEvent` 를 발행하면(phase 4 부터 Kafka
`post.created` 토픽으로 외부화), `feed` 모듈의 `@KafkaListener` 인입 어댑터가 이를 받아
`FanOutFeedUseCase` 에 위임하고, 작성자를 팔로우하는 모든 사용자의 피드에 `FeedEntry` 를
저장합니다. 이때 작성자 닉네임(이벤트가 실어온 값)과 본문 미리보기를 비정규화하여 함께
저장하므로, 피드 조회(`GetMyFeedUseCase`)는 feed 자신의 테이블만 읽고 다른 모듈을 호출하지
않습니다.

컨슈머는 발행 트랜잭션과 무관한 별도 프로세스/트랜잭션에서 실행되므로, 팬아웃이 느리거나
실패해도 게시글 생성 자체를 막거나 롤백시키지 않습니다(at-least-once). 팔로워가 매우 많은
계정에서 팬아웃 비용이 커지는 문제는 추후 하이브리드/읽기 시점 팬아웃 전략(로드맵
phase 5)으로 보완할 수 있도록 설계했습니다.

## 왜 이벤트인가

`post` 는 `PostCreatedEvent` 만 발행하고 그 뒤에 무슨 일이 일어나는지는 신경 쓰지
않습니다. `feed`, `notification`, (추후) `search` 가 각각 독립적으로 어떻게 반응할지
결정합니다(피드 팬아웃, 알림 생성, 검색 색인). 덕분에 `post` 는 다른 여러 모듈의
관심사를 알 필요가 없습니다. phase 4 에서 이 이벤트를 Kafka 로 외부화하고 반응자들을
`@KafkaListener` 컨슈머로 전환했으므로, 이제 반응자 하나를 별도 서비스로 분리하는 일은
"같은 토픽을 구독하는 별도 앱으로 옮기는" 배포 문제에 가깝지, 재작성이 아닙니다.

## MSA 분리 후보

**`feed`** 가 1순위 후보입니다. 이미 `follow`/`post` 의 공개 API 와 이벤트만으로
시스템의 나머지와 소통하고, 읽기 중심/지연에 민감한 트래픽 특성상 독립 확장과 전용
캐시 계층(Redis, 로드맵 phase 5)의 이득이 가장 큽니다. `search` 는 Elasticsearch
색인이 들어오면 그다음 후보가 됩니다.

## 로컬 실행

```bash
# 전체 스택(운영 형태): postgres + kafka + backend + frontend
docker compose up -d --build   # frontend :3000, backend :8080, db :5432, kafka :9092

# 개발(핫리로드): 인프라만 컨테이너로, 앱은 로컬 실행
docker compose up -d postgres kafka  # PostgreSQL + Kafka(KRaft 단일 브로커)
./gradlew bootRun                    # 앱 기동; 시작 시 schema.sql 실행
```

테스트는 `docker compose up` 이 필요 없습니다. 통합 테스트는 각자 Testcontainers 로
PostgreSQL 과 Kafka 를 띄웁니다(Docker 데몬은 실행 중이어야 함).

```bash
./gradlew test
```

## API

| Method | Path                                            | 설명            |
|--------|--------------------------------------------------|-----------------|
| POST   | `/api/members`                                   | 회원 가입       |
| GET    | `/api/members/{id}`                              | 회원 조회       |
| POST   | `/api/posts`                                     | 게시글 작성     |
| GET    | `/api/posts/{id}`                                | 게시글 조회     |
| POST   | `/api/members/{followerId}/follow/{followeeId}`  | 팔로우          |
| DELETE | `/api/members/{followerId}/follow/{followeeId}`  | 언팔로우        |
| GET    | `/api/members/{memberId}/feed`                   | 내 피드 조회    |
| GET    | `/api/members/{memberId}/notifications`          | 내 알림 조회    |

## 테스트 전략

- **UseCase 단위 테스트**(`*UseCaseTest`)는 Mockito 로 도메인 리포지토리 포트를
  목킹합니다 — Spring 컨텍스트 없이 빠르게 실행됩니다.
- **리포지토리 통합 테스트**(`*PersistenceAdapterTest`)는 `@DataJpaTest` +
  Testcontainers PostgreSQL 로 JPA 어댑터를 H2 가 아닌 실제 DB 에 대해 검증합니다.
- **`ArchitectureTest`** / **`ModularityTest`** 는 위 경계 규칙을 강제하여, 위반이
  코드 리뷰를 통과하는 대신 빌드를 실패시키도록 합니다.

## 로드맵

이 로드맵은 "인프라 기능 체크리스트"가 아니다. 각 인프라 단계에 **도메인 모델링
개선(전술적 DDD)과 결합 제거 작업을 의도적으로 끼워 넣어**, 인프라만 화려하고 도메인은
빈혈(anemic)인 비대칭 결과물이 되지 않도록 한다. 또한 phase 4 이후는 "다 구현"보다
**"왜/트레이드오프를 설명할 수 있는 깊이"** 를 우선한다 — 넓게 훑는 기술 나열보다 몇 개를
깊게 파는 편이 이 프로젝트의 학습·설명 목표에 부합한다.

**완료된 단계**

1. **v1 (MVP)** — 회원 가입/조회, 게시글 작성/조회, 팔로우/언팔로우, 내 피드 조회.
   ✅ 완료
2. **Spring Modulith 이벤트** — `PostCreatedEvent`/`MemberFollowedEvent` 발행,
   `feed`(쓰기 시점 팬아웃) 및 `notification` 리스너. ✅ 완료

3. **신뢰성 + 도메인 리치화** — Outbox 로 이벤트 발행을 안정화하면서, 동시에 도메인
   모델을 강화한다. ✅ 완료
   - Outbox: Spring Modulith 이벤트 발행 레지스트리(JDBC 기반)로 발행 신뢰성 +
     중복 처리 보완.
   - 도메인 이벤트 전환: 이벤트를 UseCase(서비스)가 직접 만들지 않고 **애그리거트가
     내부에 누적**(`PostWritten`/`MemberFollowed`)하고, UseCase 가 `pullDomainEvents()`
     로 드레인해 모듈 간 계약 이벤트로 번역·발행. DB 생성 식별자는 저장 후 `assignId()`
     로 애그리거트에 되돌려 부여(framework-free 유지, `AbstractAggregateRoot` 미사용).
   - 값 객체(VO) 도입: `Email`/`Nickname` — 도메인이 형식/불변식을 스스로 보장하고
     web DTO 의 `@Email`/`@Size` 는 제거(Result/영속성은 `.value()` 로 String 유지).
   - 타입드 ID: `MemberId`/`PostId`/`FollowId` — 각 모듈 **내부 식별자와 리포지토리
     포트만** 타입드로 하여 raw `Long` 오용을 컴파일 타임에 차단. 모듈 경계·이벤트는
     결합 회피를 위해 raw `Long` 유지.
   - 인입 어댑터 일관성: 이벤트 리스너를 `adapter.in.event` 로 분리하고 실제 작업은
     `FanOutFeedUseCase` 등 UseCase 로 옮겨, web 어댑터와 동일한 헥사고날 구조로 정리.
   - 중복 규칙 제거: 게시글 길이 제한 등은 도메인 한 곳에서만 정의.
4. **Kafka + 결합 제거** — 내부 이벤트를 외부 메시지로 확장하고, feed/notification 을
   독립 배포 가능한 Kafka 컨슈머로 전환한다. ✅ 완료
   - 이벤트 외부화: `@Externalized` 로 `post.created`/`member.followed` 토픽에 발행(기존 JDBC
     Outbox 레지스트리가 릴레이). feed/notification 은 `@KafkaListener`(독립 컨슈머 그룹)로 수신.
   - event-carried state: 이벤트 페이로드에 작성자/팔로워 nickname 을 실어 feed/notification 의
     `MemberQueryService` 동기 의존을 제거 → **feed·notification 이 더 이상 member 에 의존하지 않음**.
   - 이벤트 스키마를 공개 계약으로: 토픽명 + JSON 형태가 계약(생산자 타입 헤더 미의존), 토픽은
     생산 모듈이 `NewTopic` 으로 선언. at-least-once 전달(컨슈머 idempotency 는 phase 5).
   - Kafka Streams 는 미도입: "이벤트→DB 사이드이펙트"엔 일반 컨슈머가 맞다. 집계/조인 명분이 생기는
     phase 5 하이브리드 팬아웃에서 검토.

**계획된 단계 (인프라 + 도메인 인터리빙)**

5. **Redis + 일관성 전략** — 피드 캐시 도입. 단, **이중 쓰기(dual-write) 일관성 전략
   (write-through / 캐시 무효화)을 먼저 설계**한 뒤 진행. 팔로워가 많은 계정용
   하이브리드/읽기 시점 팬아웃 전략 실험.
6. **Elasticsearch + CQRS 명시화** — `PostCreatedEvent` 기반 검색 색인. search 를 별도
   읽기 모델(CQRS)로 명확히 하고, 원본/색인 동기화 지연·유실 대비.
7. **분리 가능성 증명** — 확립한 모듈 API / 이벤트 계약 / DB 소유권 경계를 근거로 `feed`
   분리 방안을 **ADR/문서로 증명**(필요 시 실제 서비스 분리). 실제 분리 여부보다 "분리
   가능한 구조임을 근거와 함께 설명할 수 있는 상태" 를 목표로 한다.

> 참고: 회원/게시글/팔로우 도메인은 본질적으로 CRUD 에 가까워 리치 도메인이 크게 나오지
> 않는다. phase 3 의 전술 DDD 개선은 이 한계 안에서 "도메인이 자기 규칙을 갖는다" 는 점을
> 보여주는 수준으로 하고, 억지 추상화(오버엔지니어링)는 피한다.
