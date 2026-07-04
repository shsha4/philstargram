# 전달 보장 · 멱등성 — Outbox / Inbox / 자연 멱등성 / DLQ

[← LEARNINGS 인덱스](../../LEARNINGS.md)

"정확히 한 번 효과(effectively-once)"를 만드는 생산측(유실 방지)·소비측(중복 방지) 노트.
구현은 [phase 5b](#2026-07-04--컨슈머-idempotency-구현-phase-5b)에서.

---

## 2026-07-03 — 멱등성 · Outbox/Inbox · DLQ (phase 4 후속 토론)

### 1. Kafka 에는 유니크 제약이 없다 — 중복 방지는 소비자 책임

- Kafka 는 append-only 로그. 같은 내용을 100번 넣으면 100개가 쌓인다. **내용 기반 유니크/거부 없음.**
- "정확히 한 번 처리" 책임은 **항상 컨슈머(와 그 저장소)** 에 있다.
- 헷갈리기 쉬운, "비슷해 보이지만 유니크 제약 아님":
  - **Producer 멱등성**(`enable.idempotence=true`): 프로듀서 **재시도** 중복 발행만 방지
    (producerId+시퀀스). 컨슈머 중복 소비는 못 막음.
  - **로그 컴팩션**: 키별 마지막 레코드만 남기는 **백그라운드 청소**(거부 아님). 컴팩션 전엔 중복 배달됨.

### 2. 컨슈머 멱등성 두 방법 — 택1 (섞는 게 아님)

두 방법의 **차이는 "이벤트/eventId 를 저장하느냐"**:

**A. 자연 멱등성 — 이벤트 저장 안 함**
- 연산 자체가 두 번 해도 결과가 같게. 중복 흡수는 **비즈니스 테이블 자신의 유니크키**.
- 예) `feed_entries UNIQUE(owner_member_id, post_id)` + `INSERT ... ON CONFLICT DO NOTHING`.
- 추가 테이블/쓰기/정리(TTL) 없음. **가장 쌈.** sink 이 Redis(`SET NX`)/ES(문서 id) 여도 동일 개념.
```
컨슘 → 비즈니스 쓰기(ON CONFLICT DO NOTHING) → 커밋   (이벤트 저장 X, 상태 update X)
```

**B. 인박스 패턴 — event_id 저장**
- 비즈니스 테이블에 자연 유니크키가 없을 때. 중복 흡수는 **inbox 테이블의 event_id PK**.
```
컨슘 → [TX] inbox 에 event_id insert(PK 충돌=이미 처리=skip) + 비즈니스 처리 → [커밋]
```
- **원자성**: inbox insert 와 비즈니스 쓰기는 **같은 트랜잭션**.
- **event_id 로우의 존재 자체 = 처리 완료** (별도 "상태 완료" 컬럼 불필요 — 아웃박스와 비대칭).
- 전제: 이벤트 계약에 안정적인 `eventId`(UUID) 가 있어야 함.

**선택 기준**: 비즈니스 테이블에 자연 유니크키 있으면 A(더 쌈), 없으면 B.

### 3. Outbox(생산) ↔ Inbox(소비) 는 한 세트 = effectively-once

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

### 4. 실패 처리 3가지 방식 + DLQ ⟂ 멱등성

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

### 오해 교정
- "Kafka 에 유니크 제약" → ❌. 유니크 제약은 **소비자 쪽 DB(sink)** 에 있는 것.
- "자연 멱등성 = 이벤트 로우 적재" → ❌. 자연 멱등성은 **아무것도 적재 안 함**; 이벤트 로우를
  적재하는 건 **인박스 패턴**(둘은 택1).
- "DLQ 구현하려면 인박스 필요" → ❌. **직교.** DLQ 는 에러 핸들러/토픽으로 독립 구현.

---

## 2026-07-04 — 컨슈머 idempotency 구현 (phase 5b) — 위 2번 결정의 실제 적용

phase 4 후속 토론(위 2번)에서 정한 "자연 멱등성(A) vs 인박스(B)"를 실제로 붙였다.

### feed = 순수 자연 멱등성(A)
- `feed_entries UNIQUE(owner_member_id, post_id)` + native `INSERT ... ON CONFLICT DO NOTHING`.
- **JPA `save()` 를 쓰면 안 되는 이유**: 충돌 시 `save()` 는 예외를 던져 **트랜잭션을 abort** 시킨다.
  팬아웃은 팔로워 루프라 한 건 충돌이 전체 트랜잭션을 깨면 안 됨 → `@Modifying` native 쿼리로 우회.
  포트 `FeedRepository.save` 반환을 `void` 로 바꿔 "멱등 삽입"임을 시그니처로 드러냈다.

### notification = "파생 자연키"로 A 를 적용(예상했던 B 대신)
- 위 2번은 notification 을 "자연 유니크키 없음 → 인박스(B)" 후보로 봤다. 실제로는 **별도 inbox(event_id)
  테이블 없이**, 이벤트 내용으로 **결정적 dedup_key `type:recipient:sourceId`** 를 만들어 notifications
  **자기 테이블**에 유니크키로 얹었다(`NEW_POST:{follower}:{postId}`, `NEW_FOLLOWER:{followee}:{follower}`).
- 즉 인박스(추가 테이블·조인)를 피하고 **A 방식(비즈니스 테이블 자신의 유니크키)에 그대로 얹은** 것.
  **전제**: 소스 이벤트가 알림을 결정적으로 식별할 수 있어야 함(우리는 가능) → 이게 성립하면 B 보다 쌈.
  성립 안 하는 경우(예: eventId 만 있고 내용 매핑이 불가)에만 인박스(B)가 필요.
- 이를 위해 `PostCreatedEvent`→`NotifyNewPostCommand` 에 **postId 를 전달**하도록 확장(기존엔 authorId 만
  넘겨 게시글 식별이 불가했음).

### Kafka Streams 집계의 멱등성은 별도
- follower-count 집계(phase 5c)의 멱등성은 "관계 키 기준 upsert/tombstone KTable" 로 모델링해 +1/-1
  누적의 중복 문제를 구조적으로 없앤다 — 같은 A(자연 멱등)의 스트림 버전.
  → [하이브리드 팬아웃 · Kafka Streams](hybrid-fanout-kafka-streams.md#3-멱등한-집계-모델링--1-1-누적을-피하라-핵심).

---

## 2026-07-05 — "파티션 키 vs 멱등성 키 vs 프로듀서 멱등성" 세 개념 구분 (Q&A)

이름이 비슷해 자주 섞이는 세 가지. **전부 다른 목적·다른 위치·다른 시점**이다.

| | 파티션 키 | 멱등성 키(dedup_key) | 프로듀서 멱등성 |
|---|---|---|---|
| 목적 | **순서 보장**(+부하 분산) | **소비 중복 실행 방지** | **생산 재시도 중복 방지** |
| 위치/주체 | 프로듀서 Kafka 라우팅 | 소비자/싱크 DB 유니크키 | 프로듀서 내부(producerId+시퀀스) |
| Kafka 가 아는가 | O(라우팅) | **X**(dedup 존재도 모름) | O(`enable.idempotence`) |
| 우리 예 | `post.created::#{authorId}` | `UNIQUE(owner,post)`, `dedup_key` | (미사용) |

- **파티션 키** = "특정 파티션으로 라우팅 → 파티션 단위 순서가 필요할 때". 상세는
  [Kafka 토픽·파티션·순서](kafka-topic-partition-ordering.md).
- **멱등성 키(dedup_key)** = "이벤트 중복으로 비즈니스 로직이 중복 실행되는 걸 막는 키". Kafka 는 모르고
  **소비자 DB** 에서 유니크키로 흡수.
- 같은 `post.created` 하나에 대해 두 키가 **전혀 다른 필드**를 쓴다(파티션=authorId, 멱등=owner/post 또는
  dedup_key) → 시점(생산 라우팅 vs 소비 반영)도 위치도 독립.
- **프로듀서 멱등성**은 또 다른 것: 프로듀서가 재시도하다 브로커에 같은 메시지를 두 번 쓰는 것만 막음.
  컨슈머 중복 처리는 못 막으므로 dedup_key 를 대체하지 못한다.
