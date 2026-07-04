# LEARNINGS

phase 별 작업 중 나눈 설계 토론에서 나온 **학습 노트**. 코드 변경 이력이 아니라
"왜/트레이드오프를 설명할 수 있는" 개념 정리가 목적이다(프로젝트 원칙: 넓이보다 깊이).

---

## 2026-07-03 — Kafka 토픽 설계 · 파티션 키 · 멱등성 · Outbox/Inbox (phase 4 후속 토론)

### 1. 토픽 단위를 정하는 1순위 기준 = "순서 보장이 필요한 이벤트인가"

- 토픽 granularity 의 판단 축은 **"잘게 쪼갬 vs 뭉침"(미학)** 이 아니라
  **"순서가 중요한 이벤트들을 같은 토픽·파티션에 두었는가"** 다.
- Kafka 순서 보장은 **파티션 안에서만**. 두 이벤트의 순서를 보장하려면 **둘 다** 필요:
  1. **같은 토픽** (다른 토픽끼리는 키가 같아도 순서 무관)
  2. **같은 파티션 키** (같은 키 → 같은 파티션)
- 규칙: **한 애그리거트의 생명주기 이벤트(created→updated→deleted)는 순서가 중요하므로
  같은 토픽에 두고 애그리거트 id 로 키잉**한다. 그 외에는 나눠도 된다.
  - 예) `post.created` 와 `post.deleted` 를 다른 토픽에 두면, 부하 시 삭제가 생성보다
    먼저 처리될 수 있다(토픽 간 순서 없음) → 색인/피드가 깨짐.

**순서 = "묶는 힘", 그 외 요소 = "떼는 힘"**:
- 떼는 힘: 접근 제어(ACL) 경계, retention/compaction 정책, 처리량/스케일, PII·스키마 격리.
- → 순서로 묶을 단위를 정하고, 위 이유가 강하면 뗀다.

### 2. 토픽 vs 파티션 키는 별개의 결정 — 순서를 지키는 건 "키"

- **토픽** = 이벤트를 담는 큰 컨테이너.
- **파티션 키** = 실제로 순서를 결정하는 것.
- `post` 토픽을 파티션 10개로 잡아도 postId 로 키잉하면 한 게시글의 사건들은 한 파티션 = 순서 보장.
  토픽을 안 쪼개도 순서는 키가 지킨다.

### 3. 키의 단위: 애그리거트(= 트랜잭션/일관성 경계)가 기본값, 최종은 "소비자 관점"

- DDD: **애그리거트 = 일관성(불변식) 경계 = 트랜잭션 경계** ("한 트랜잭션은 한 애그리거트만 수정").
- 각 트랜잭션은 한 애그리거트의 상태 변화 하나를 만들고, 그 인스턴스의 변화들은 순차 시퀀스다
  → **애그리거트 id 를 파티션 키로 잡는 게 자연스러운 기본값.**
- 단, 절대 규칙은 아님. 진짜 질문은 **"이 소비자에게 순서가 유지돼야 하는 범위가 무엇인가"**:
  - 보통 = 애그리거트 id
  - 가끔 더 넓게 = userId(한 사용자의 활동 전체 순서)
  - 우리 프로젝트 실제 사례가 이 예외에 해당(아래 4).

### 4. 우리 프로젝트가 실제로 잡은 파티션 키

`@Externalized("토픽::#{키}")` 의 SpEL 부분:

| 토픽 | 키 | 근거(소비자 관점) |
|---|---|---|
| `post.created` | **authorId** | 한 작성자의 게시글이 피드에 들어가는 순서(feed) |
| `member.followed` | **followeeId** | 한 사람에게 도착하는 팔로우 이벤트 순서(notification) |

- 즉 post 애그리거트인데 postId 가 아니라 **authorId**(소비자=feed 의 순서 범위)로 키를 잡은,
  "키가 애그리거트 id 가 아닌" 사례.
- ⚠️ **숨은 트레이드오프**: `PostUpdated`/`PostDeleted` 가 생기면 한 게시글의 생명주기 순서를
  보장하려면 **postId 키**가 맞다. authorId 키로는 특정 게시글의 순서를 보장 못 함.
  두 목표(작성자 타임라인 순서 vs 게시글 생명주기 순서)는 한 토픽·한 키로 동시에 못 잡는다.
  현재는 feed 가 `created_at` 으로 정렬해 읽어 안전하지만, **post 수정/삭제 도입 시 키 재검토 필요.**

### 5. Kafka 에는 유니크 제약이 없다 — 중복 방지는 소비자 책임

- Kafka 는 append-only 로그. 같은 내용을 100번 넣으면 100개가 쌓인다. **내용 기반 유니크/거부 없음.**
- "정확히 한 번 처리" 책임은 **항상 컨슈머(와 그 저장소)** 에 있다.
- 헷갈리기 쉬운, "비슷해 보이지만 유니크 제약 아님":
  - **Producer 멱등성**(`enable.idempotence=true`): 프로듀서 **재시도** 중복 발행만 방지
    (producerId+시퀀스). 컨슈머 중복 소비는 못 막음.
  - **로그 컴팩션**: 키별 마지막 레코드만 남기는 **백그라운드 청소**(거부 아님). 컴팩션 전엔 중복 배달됨.

### 6. 컨슈머 멱등성 두 방법 — 택1 (섞는 게 아님)

두 방법의 **차이는 "이벤트/eventId 를 저장하느냐"**:

**A. 자연 멱등성 — 이벤트 저장 안 함**
- 연산 자체가 두 번 해도 결과가 같게. 중복 흡수는 **비즈니스 테이블 자신의 유니크키**.
- 예) `feed_entries UNIQUE(owner_member_id, post_id)` + `INSERT ... ON CONFLICT DO NOTHING`.
- 추가 테이블/쓰기/정리(TTL) 없음. **가장 쌈.** sink 이 Redis(`SET NX`)/ES(문서 id) 여도 동일 개념.
```
컨슘 → 비즈니스 쓰기(ON CONFLICT DO NOTHING) → 커밋   (이벤트 저장 X, 상태 update X)
```

**B. 인박스 패턴 — event_id 저장**
- 비즈니스 테이블에 자연 유니크키가 없을 때(예: notification). 중복 흡수는 **inbox 테이블의 event_id PK**.
```
컨슘 → [TX] inbox 에 event_id insert(PK 충돌=이미 처리=skip) + 비즈니스 처리 → [커밋]
```
- **원자성**: inbox insert 와 비즈니스 쓰기는 **같은 트랜잭션**.
- **event_id 로우의 존재 자체 = 처리 완료** (별도 "상태 완료" 컬럼 불필요 — 아웃박스와 비대칭).
- 전제: 이벤트 계약에 안정적인 `eventId`(UUID) 가 있어야 함.

**선택 기준**: 비즈니스 테이블에 자연 유니크키 있으면 A(더 쌈), 없으면 B.

### 7. Outbox(생산) ↔ Inbox(소비) 는 한 세트 = effectively-once

**Outbox (생산측) — dual-write 문제 해결**
- DB 커밋과 Kafka 전송은 한 트랜잭션으로 못 묶음(다른 시스템). 그래서 전송 대상을 **같은 DB의
  아웃박스 테이블에 원자적으로 적어두고**, 전송은 재시도 가능한 별도 단계로 뺀다.
```
[TX] 비즈니스 쓰기 + 아웃박스 이벤트로우 insert   ← 원자적으로 함께 커밋
[커밋]
→ (커밋 후) 릴레이가 아웃박스 읽어 Kafka 발행 → 발행 성공 시 완료로 로우 update
```
- 앱 코드는 1+2(DB 쓰기)만 한다. **진짜 Kafka 발행은 커밋 후 별도 릴레이.**
- 아웃박스는 **2단계 상태**(발행 대기 → 완료)를 가진다.
- 우리 구현: `event_publication` 테이블 = 아웃박스, Modulith externalizer = 릴레이,
  `completion_date` = 완료 표시. `republish-outstanding-events-on-restart` 로 재시작 시 미완료 재발행.

**대칭 정리**
```
[생산 · 아웃박스]  [TX] 비즈니스 + 이벤트로우 insert → [커밋] → Kafka 발행 → 이벤트로우 완료 update
[소비 · 택1]
   A. 자연 멱등성:  컨슘 → 비즈니스 쓰기(ON CONFLICT DO NOTHING) → 커밋        (이벤트 저장 X)
   B. 인박스:      컨슘 → [TX] event_id insert(충돌=skip) + 비즈니스 처리 → [커밋]
```
- **비대칭 주의**: 아웃박스는 2단계 상태(대기→완료), 인박스는 "존재=완료"의 1단계.
- **한 문장**: 아웃박스(생산=유실 방지) ↔ 멱등성/인박스(소비=중복 방지) 가 한 세트로
  **"정확히 한 번 효과(effectively-once)"** 를 만든다. (릴레이가 발행 후 완료표시 직전에 죽으면
  재발행 → at-least-once → 그래서 소비측 멱등성이 필요하다는 인과.)

### 핵심 오해 교정 모음(이번 토론에서 실제로 헷갈렸던 것)
- "토픽 잘게 쪼개면 오버엔지니어링" → ❌. 판단축은 granularity 가 아니라 **순서 보장 범위**.
- "Kafka 에 유니크 제약" → ❌. 유니크 제약은 **소비자 쪽 DB(sink)** 에 있는 것.
- "자연 멱등성 = 이벤트 로우 적재" → ❌. 자연 멱등성은 **아무것도 적재 안 함**; 이벤트 로우를
  적재하는 건 **인박스 패턴**(둘은 택1).

### 8. 컨슈머 그룹(`@KafkaListener` 의 `groupId`)

- `groupId` = **컨슈머 그룹**. Kafka 의 **오프셋 추적 단위이자 확장 단위**.
- **규칙 1 — 같은 그룹끼리는 파티션을 "나눠 가진다"**(부하 분산/병렬). 각 파티션은 그룹 내
  정확히 한 컨슈머에게만 배정 → **각 메시지는 그룹당 딱 한 번** 처리.
- **규칙 2 — 다른 그룹끼리는 "각자 전부 받는다"**(팬아웃). 그룹마다 오프셋을 독립 추적.
- 우리 설계: feed·notification 은 `post.created` 를 **둘 다** 받아야 하므로 **다른 그룹**(`feed`,
  `notification`) → 팬아웃. 만약 같은 그룹이면 게시글 하나가 한쪽에만 가서 깨진다(흔한 실수).
- **수평 확장**: 같은 서비스를 N대 띄우고 **같은 그룹**으로 두면 파티션이 나뉘어 병렬 처리(메시지는
  그룹당 1번). 단 **병렬 상한 = 파티션 수** — 우리는 파티션 1개라 그룹당 1개만 일하고 나머지는 대기.
  더 늘리려면 파티션↑(대신 순서 보장 범위가 파티션 단위로 쪼개짐 — 3번과 연결).

### 9. 오프셋 커밋: raw Kafka vs Spring Kafka

- **raw Kafka** = 순수 `org.apache.kafka.clients.consumer.KafkaConsumer` 를 직접 poll/commit 하는 것.
  기본 `enable.auto.commit=true` → 5초마다 poll 시점에 커밋 → **처리 성공과 무관하게 오프셋 전진**
  → 처리 중 죽으면 유실 위험.
- **Spring Kafka(`@KafkaListener`)** = 리스너 컨테이너가 poll/commit 을 대신. **auto-commit 을 꺼고**
  (`enable.auto.commit=false`) **리스너 메서드가 예외 없이 반환한 뒤 커밋**(기본 `AckMode=BATCH`).
- **우리 코드엔 커밋 코드가 없다.** `@KafkaListener` 메서드가 **정상 반환하는 것 = 성공 신호**이고,
  커밋은 Spring `KafkaMessageListenerContainer` 내부가 수행. 예외를 던지면 **커밋 안 하고**
  `DefaultErrorHandler` 가 되감아 재시도.
- 수동 제어하려면 `AckMode.MANUAL` + `Acknowledgment ack` 파라미터로 `ack.acknowledge()` 호출.
  우리는 안 씀(자동에 위임).

### 10. 실패 처리 3가지 방식 + DLQ ⟂ 멱등성

- **DLQ(Dead Letter Queue) 와 인박스는 직교(orthogonal).** DLQ 는 인박스 없이도 구현된다.
  - DLQ = "N번 재시도해도 실패한 메시지를 치워두는 곳"(보통 별도 Kafka 토픽 `.DLT`).
  - 인박스 = 중복 처리 방지(멱등성). **서로 다른 문제.**
- 관계의 진짜 알맹이: **재시도/DLQ replay 는 같은 메시지를 다시 처리 → 멱등성이 있어야 안전.**
  그 멱등성이 **자연 멱등이면 인박스 불필요**, 자연 멱등이 안 될 때만 인박스가 필요.
  즉 "DLQ 하려면 인박스" 가 아니라 **"DLQ replay → 멱등성 필요 → (자연 멱등 불가 시) 인박스"**.

**실패 처리 3방식 비교:**

| 목표 | 방식 |
|---|---|
| 단순 재시도+DLQ, 순서 유지, 코드 최소 | **Kafka-native 블로킹**: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` |
| 파티션 안 막히게 + Kafka 로만 | **`@RetryableTopic`**: 지연 재시도 토픽 계층 + `.DLT` (논블로킹) |
| 수신/처리 분리 + 재시도·DLQ 를 DB로 조회·replay | **인박스 + DB 워커 + DB DLQ**(순서·복잡도 비용 감수) |

- 블로킹 재시도는 poison 메시지가 파티션을 붙잡음 → `@RetryableTopic` 이나 인박스로 회피.
- 인박스+워커는 정식 패턴이나, **Kafka 파티션 순서를 잃고 DB 위에 큐를 재구현**하는 비용.
  "유실 방지"가 목적이면 Spring 이 이미 auto-commit 을 꺼 해결하고, "파티션 안 막힘"이 목적이면
  `@RetryableTopic` 이 DB 없이 같은 걸 준다 → 인박스+워커는 **DB 중심 운영 가시성/처리 분리**가
  진짜 요구일 때만.

### 추가 오해 교정
- "별도 설정 없으면 오토커밋이라 컨슘하면 오프셋 밀림" → raw Kafka 는 맞지만 **Spring Kafka 는
  auto-commit 을 꺼고 성공 후 커밋**한다(우리 환경). 실패건은 오프셋 전진 안 함.
- "DLQ 구현하려면 인박스 필요" → ❌. **직교.** DLQ 는 에러 핸들러/토픽으로 독립 구현.
- "같은 컨슈머 그룹이면 팬아웃" → ❌. **같은 그룹 = 나눠 가짐**, 다른 그룹 = 각자 전부(팬아웃).

---

## 2026-07-04 — Redis 피드 캐시 · 이중 쓰기 일관성 전략 (phase 5a)

### 1. 캐시 일관성의 출발점 = "진실의 원천을 하나로 못박기"

- 캐시를 붙이는 순간 같은 데이터가 **두 저장소(Postgres, Redis)** 에 존재 → 둘이 어긋날 수 있다
  (dual-write inconsistency). 분산 트랜잭션은 안 쓴다(2PC 는 비싸고 가용성을 깎음).
- 대신 **Postgres = 진실의 원천, Redis = 버려도 되는 사본(disposable)** 으로 못박는다.
  그러면 "어긋남"은 **정합성 오류가 아니라 캐시 staleness(신선도)** 문제로 격하된다 —
  Postgres 만 맞으면 캐시는 언제든 재생성 가능.

### 2. 우리가 고른 조합: cache-aside 읽기 + write-through-if-present 쓰기 + fail-safe evict

| 경로 | 전략 | 이유 |
|---|---|---|
| 읽기 | **cache-aside**: 미스면 Postgres 읽어 재적재 | 캐시가 죽어도 미스로 저하될 뿐 정답을 서빙 |
| 쓰기(팬아웃) | **write-through, 단 캐시가 있을 때만 append** | 피드 쓰기는 "1건 append" → 덧붙이는 게 자연스럽고 쌈 |
| 실패 시 | **evict(DEL)** 후 다음 읽기에 재적재 | 부분 실패한 캐시를 남기지 않음(자가 치유) |

- **핵심 통찰:** 진실이 Postgres 에 있으니 이중 쓰기가 유실돼도(예: Redis append 실패) TTL 만료나
  다음 미스 시점에 **자동 복구**된다 → 일관성은 "결과적 + TTL 로 상한이 걸린 staleness", 정답성은
  항상 보장(bounded staleness).

### 3. write-through-append vs invalidate-on-write — 피드에선 append 가 맞다

- 일반 캐시 갱신 정석은 "쓰면 캐시 무효화(invalidate)". 그런데 **피드 팬아웃에 그대로 쓰면 안 됨**:
  셀럽이 글 하나 쓰면 팔로워 N 명의 피드 키가 **동시에 무효화** → 그들이 접속하는 순간 N 개의 캐시
  미스가 한꺼번에 Postgres 를 때린다(**thundering herd**).
- 피드 팬아웃은 본질적으로 "타임라인 맨 앞에 1건 추가"라 **append(ZADD) 가 무효화보다 싸고 정확**.
  그래서 append 를 정상 경로로, invalidate 는 **쓰기 실패 시 폴백**으로만 쓴다.

### 4. "캐시가 있을 때만 채운다(write-through-if-present)" = 콜드 캐시 안 데움

- 팬아웃 시 **모든** 팔로워 캐시를 채우면, 최근 접속도 안 한 유저 피드까지 Redis 에 실체화
  → 쓰기 증폭 + 메모리 낭비.
- 그래서 **해당 팔로워의 캐시 키가 이미 있을 때만** append. 콜드 유저는 다음에 직접 읽을 때
  cache-aside 로 채워진다(lazy). "활성 유저만 뜨겁게 유지" 하는 정책.

### 5. 구현 디테일(우리 코드)

- 자료구조: **ZSET** `feed:{memberId}`, score=`createdAt` epoch millis, member=`FeedItem` JSON.
  최신순은 `ZREVRANGE`, 크기 제한은 `ZREMRANGEBYRANK`(상위 N 만 유지).
- **빈 피드의 함정**: ZSET 은 멤버가 0개면 키가 사라진다 → "글 없는 피드"는 캐시로 표현 못 하고
  매번 미스(저렴한 Postgres 조회)로 처리. 의도적 트레이드오프.
- **캐시는 요청을 절대 못 깨게**: 모든 Redis 접근을 try/catch 로 감싸 예외를 삼킨다. 읽기 실패=미스
  처리(Postgres 폴백), 쓰기 실패=evict. 캐시 계층은 graceful degradation 이 원칙.
- 헥사고날: `FeedCache` 아웃바운드 포트(application) + `RedisFeedCache` 어댑터(adapter.out.cache).
  domain 은 Redis 를 전혀 모른다(ArchUnit `domainPackagesStayFrameworkFree` 유지).
- 스택: Boot 가 자동설정한 `StringRedisTemplate` + Jackson 3(`tools.jackson`) `ObjectMapper`.
  테스트는 `GenericContainer("redis:7-alpine")` + `@ServiceConnection(name="redis")`(이미지명으로
  Boot 의 Redis ConnectionDetailsFactory 가 매칭 → `spring.data.redis.*` 자동 주입).

---

## 2026-07-04 — 컨슈머 idempotency 구현 (phase 5b) — 위 6번 결정의 실제 적용

phase 4 후속 토론(위 6번)에서 정한 "자연 멱등성(A) vs 인박스(B)"를 실제로 붙였다.

### 1. feed = 순수 자연 멱등성(A)
- `feed_entries UNIQUE(owner_member_id, post_id)` + native `INSERT ... ON CONFLICT DO NOTHING`.
- **JPA `save()` 를 쓰면 안 되는 이유**: 충돌 시 `save()` 는 예외를 던져 **트랜잭션을 abort** 시킨다.
  팬아웃은 팔로워 루프라 한 건 충돌이 전체 트랜잭션을 깨면 안 됨 → `@Modifying` native 쿼리로 우회.
  포트 `FeedRepository.save` 반환을 `void` 로 바꿔 "멱등 삽입"임을 시그니처로 드러냈다.

### 2. notification = "파생 자연키"로 A 를 적용(예상했던 B 대신)
- 위 6번은 notification 을 "자연 유니크키 없음 → 인박스(B)" 후보로 봤다. 실제로는 **별도 inbox(event_id)
  테이블 없이**, 이벤트 내용으로 **결정적 dedup_key `type:recipient:sourceId`** 를 만들어 notifications
  **자기 테이블**에 유니크키로 얹었다(`NEW_POST:{follower}:{postId}`, `NEW_FOLLOWER:{followee}:{follower}`).
- 즉 인박스(추가 테이블·조인)를 피하고 **A 방식(비즈니스 테이블 자신의 유니크키)에 그대로 얹은** 것.
  **전제**: 소스 이벤트가 알림을 결정적으로 식별할 수 있어야 함(우리는 가능) → 이게 성립하면 B 보다 쌈.
  성립 안 하는 경우(예: eventId 만 있고 내용 매핑이 불가)에만 인박스(B)가 필요.
- 이를 위해 `PostCreatedEvent`→`NotifyNewPostCommand` 에 **postId 를 전달**하도록 확장(기존엔 authorId 만
  넘겨 게시글 식별이 불가했음).

### 3. Kafka Streams 집계의 멱등성은 5c 에서 별도로(스포일러)
- 컨슈머 쓰기 멱등성(위)과 별개로, follower-count 집계(5c)의 멱등성은 "관계 키 기준 upsert/tombstone
  KTable" 로 모델링해 +1/-1 누적의 중복 문제를 구조적으로 없앤다. 같은 A(자연 멱등)의 스트림 버전.

---

## 2026-07-04 — 하이브리드 팬아웃 · Kafka Streams follower-count (phase 5c)

### 1. 왜 하이브리드 팬아웃인가 — 쓰기 팬아웃의 아킬레스건

- 기본 전략(fan-out-on-write)은 읽기를 싸게 만드는 대신 **쓰기 비용이 팔로워 수에 비례**한다.
  팔로워 수백만 셀럽이 글 하나 쓰면 수백만 row 를 쓴다(write amplification).
- 해법: **셀럽만 예외 처리.** 셀럽 글은 쓰기 팬아웃을 **건너뛰고**, 팔로워가 조회할 때 셀럽의 최근
  글을 **읽기 시점에 pull** 해 materialized 피드와 병합(fan-out-on-read). 일반 유저는 그대로 push.
- 트레이드오프: 셀럽 글은 읽을 때마다 pull+병합 비용(하지만 셀럽 수는 적고, 읽기 시점 몇 개 join)
  ↔ 쓰기 시점 수백만 write 회피. 읽기 지연을 조금 얹어 쓰기 폭발을 없애는 교환.

### 2. 셀럽 판별에 Kafka Streams(KTable)를 쓴 이유 — 그리고 오버엔지니어링 경계

- 판별 = follower-count ≥ 임계값. **이 규모면 `SELECT count(*) FROM follows WHERE followee_id=?`
  로도 충분하다** — 솔직히 Streams 는 필요조건이 아니다.
- Streams 를 쓴 명분: (a) 셀럽 판별은 **모든 게시글 작성/조회의 핫패스**라 DB count 를 반복하기보다
  **상시-최신 집계를 메모리(상태 저장소)에** 두는 게 맞고, (b) phase 4 에서 미룬 "집계/상태라는 실제
  명분이 생기는 지점"이 바로 여기다(KTable + interactive query 를 실제로 태워봄).
- **경계 명시**: 그 이상(조인 폭증, 윈도우, 복잡 토폴로지)은 이 도메인에 명분이 없어 안 한다.

### 3. 멱등한 집계 모델링 — +1/-1 누적을 피하라 (핵심)

- 순진한 방법: follow=+1, unfollow=-1 누적. **문제**: at-least-once 재전달 시 같은 follow 가 두 번
  오면 +2 가 되어 카운트가 틀어진다(5번의 "컨슈머 중복" 문제가 집계에서 재현).
- 우리 모델: **관계 키(`follower:followee`)를 키로 하는 KTable**.
  - follow → 그 키에 값 upsert, unfollow → 그 키를 **tombstone(null)** 로 삭제.
  - 그 위에서 `groupBy(followee).count()`. KTable 집계는 키의 이전 값을 알고 감산/가산하므로,
    같은 관계가 재전달돼도 **같은 키 upsert 라 카운트가 두 번 오르지 않고**, tombstone 은 정확히 -1.
- 즉 5b 의 "자연 멱등성(A)"을 **스트림 버전**으로 옮긴 것 — "누적"이 아니라 "관계의 현재 상태"를 KTable
  로 두면 멱등이 공짜로 따라온다. `TopologyTestDriver` 로 집계·재전달 멱등·tombstone 감산을 결정 검증.

### 4. 헥사고날 + interactive query — Streams 타입을 application 에서 격리

- 집계 토폴로지는 `follow.adapter.out.stream`(어댑터), 셀럽 판별 API 는 `follow.application`.
- application 이 Kafka Streams 타입(`KafkaStreams`, 상태 저장소)에 직접 의존하지 않도록
  **`FollowerCountView` 아웃바운드 포트**를 두고, interactive query 구현을 어댑터에 숨겼다.
  포트는 `Optional<Long>` 을 돌려주고, 스토어 미준비(기동/리밸런스)면 empty → 호출측이 "셀럽 아님"으로
  안전 처리(**결과적 일관성 하에서 정답성 우선** — 잘못 셀럽 판정해 팬아웃을 깨느니 일반 처리).

### 5. 읽기 pull 이 phase 4 결합 제거를 깨지 않게 — post 에 닉네임 스냅샷

- 읽기 시점 pull 은 셀럽의 글을 `post` 에서 가져오는데, 피드엔 **작성자 닉네임**이 필요하다.
  여기서 `feed → member` 를 다시 부르면 phase 4 에서 없앤 결합이 되살아난다.
- 해결: `post` 가 작성 시점에 이미 조회하던 작성자 닉네임을 **`posts.author_nickname` 스냅샷**으로
  저장. 그러면 pull 이 **`feed → post` 하나로 완결**되고 `feed → member` 는 여전히 없다.
  (feed 가 이미 하던 닉네임 비정규화와 같은 결의 결정 — 읽기 모델은 스냅샷을 들고 다닌다.)

### 6. 언팔로우 이벤트 도입 — 아키텍처 비대칭 한 곳

- 집계가 -1 을 반영하려면 언팔로우도 이벤트가 필요해 `MemberUnfollowedEvent` 를 추가.
- follow/write 는 애그리거트가 도메인 이벤트를 raise 하지만, unfollow 는 키로만 삭제(애그리거트 로드
  없음)라 **UseCase 에서 직접 발행**했다. 의도적 비대칭 — 없는 애그리거트를 로드하는 억지보다 낫다.
