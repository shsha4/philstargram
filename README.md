# philstargram — SNS 모듈러 모놀리스

백엔드 학습용 프로젝트입니다. 단순 CRUD SNS 가 아니라, 하나의 Spring Boot
애플리케이션 안에서 모듈 경계를 충분히 엄격하게 유지하여 **필요하면 어떤 모듈이든
별도 서비스로 분리(MSA)** 할 수 있도록 만드는 연습입니다.

## 기술 스택

Java 21, Spring Boot 4.1, Spring Modulith, Spring Data JPA, PostgreSQL,
Testcontainers, ArchUnit. Redis / Kafka / Elasticsearch / Outbox 는 아래 로드맵에
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
   `post`, `follow` → `member`; `feed`, `notification` → `member`, `follow`.

모듈별 헥사고날 흐름: `adapter.in.web → application → domain`,
`adapter.out.persistence → application/domain`, 그리고 `domain` 은 바깥을 향하지
않습니다.

경계 규칙은 손으로 작성한 `ArchitectureTest`(ArchUnit)와 Spring Modulith 의
`ApplicationModules.verify()`(`ModularityTest`) 두 곳에서 교차 검증됩니다.

## 피드 전략

기본 전략은 **쓰기 시점 팬아웃(fan-out-on-write)** 입니다. SNS 에서는 피드 조회가
쓰기보다 훨씬 잦으므로, 게시글 작성 시점에 팔로워들의 피드에 미리 적재하여 읽기
성능을 우선합니다.

동작 방식: `CreatePostUseCase` 가 `PostCreatedEvent` 를 발행하면, `feed` 모듈의
`FeedFanOutOnPostCreated`(`@ApplicationModuleListener`)가 이를 받아 작성자를
팔로우하는 모든 사용자의 피드에 `FeedEntry` 를 저장합니다. 이때 작성자 닉네임과 본문
미리보기를 비정규화하여 함께 저장하므로, 피드 조회(`GetMyFeedUseCase`)는 feed 자신의
테이블만 읽고 다른 모듈을 호출하지 않습니다.

리스너는 발행 트랜잭션이 커밋된 뒤 별도 트랜잭션에서 비동기로 실행되므로, 팬아웃이
느리거나 실패해도 게시글 생성 자체를 막거나 롤백시키지 않습니다. 팔로워가 매우 많은
계정에서 팬아웃 비용이 커지는 문제는 추후 하이브리드/읽기 시점 팬아웃 전략(로드맵
phase 5)으로 보완할 수 있도록 설계했습니다.

## 왜 이벤트인가

`post` 는 `PostCreatedEvent` 만 발행하고 그 뒤에 무슨 일이 일어나는지는 신경 쓰지
않습니다. `feed`, `notification`, (추후) `search` 가 각각 독립적으로 어떻게 반응할지
결정합니다(피드 팬아웃, 알림 생성, 검색 색인). 덕분에 `post` 는 다른 여러 모듈의
관심사를 알 필요가 없고, 나중에 이 반응자(reactor)들 중 하나를 별도 서비스로 분리할
때도 인메모리 리스너를 Kafka 컨슈머로 바꾸는 정도로 끝나지, 재작성이 필요하지
않습니다.

## MSA 분리 후보

**`feed`** 가 1순위 후보입니다. 이미 `follow`/`post` 의 공개 API 와 이벤트만으로
시스템의 나머지와 소통하고, 읽기 중심/지연에 민감한 트래픽 특성상 독립 확장과 전용
캐시 계층(Redis, 로드맵 phase 5)의 이득이 가장 큽니다. `search` 는 Elasticsearch
색인이 들어오면 그다음 후보가 됩니다.

## 로컬 실행

```bash
docker compose up -d          # localhost:5432 에 PostgreSQL 기동
./gradlew bootRun             # 앱 기동; 시작 시 schema.sql 실행
```

테스트는 `docker compose up` 이 필요 없습니다. DB 를 건드리는 테스트는 각자
Testcontainers PostgreSQL 을 띄웁니다.

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

1. **v1 (MVP)** — 회원 가입/조회, 게시글 작성/조회, 팔로우/언팔로우, 내 피드 조회.
   ✅ 완료
2. **Spring Modulith 이벤트** — `PostCreatedEvent`/`MemberFollowedEvent` 발행,
   `feed`(쓰기 시점 팬아웃) 및 `notification` 리스너. ✅ 완료
3. Outbox 패턴으로 이벤트 발행 안정성 및 중복 처리 보완.
4. Kafka: 내부 이벤트를 외부 메시지로 확장하고, feed/notification/search 를 독립
   배포 가능한 컨슈머로 분리.
5. Redis 피드 캐시 + 팬아웃 전략 실험; 팔로워가 많은 계정용 하이브리드 전략.
6. Elasticsearch 기반 게시글 검색 색인(`PostCreatedEvent` 기반).
7. 위에서 확립한 모듈 API / 이벤트 계약 / DB 소유권 경계를 근거로 `feed` 를 별도
   서비스로 분리하는 방안 검토.
