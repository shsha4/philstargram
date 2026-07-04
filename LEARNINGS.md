# LEARNINGS

phase 별 작업 중 나눈 설계 토론에서 나온 **학습 노트**의 인덱스. 코드 변경 이력이 아니라
"왜/트레이드오프를 설명할 수 있는" 개념 정리가 목적이다(프로젝트 원칙: 넓이보다 깊이).

내용이 길어져 **주제별 파일**로 나눴다(`docs/learnings/`). 아래에서 골라 읽는다.

## 주제

- [**Kafka — 토픽 · 파티션 키 · 순서 보장**](docs/learnings/kafka-topic-partition-ordering.md)
  토픽 granularity 기준, 토픽 vs 파티션 키, 애그리거트/소비자 관점 키 선택, 컨슈머 그룹,
  오프셋 커밋. + `@Externalized` 는 Spring Modulith 문법(Kafka 아님), 파티션은 프로듀서가 결정,
  순서 보장 3단 계약, 파티션 증설 함의, 파티션 키=해시테이블 비유.

- [**전달 보장 · 멱등성 — Outbox / Inbox / 자연 멱등성 / DLQ**](docs/learnings/idempotency-and-delivery.md)
  Kafka 엔 유니크 제약 없음(소비자 책임), 컨슈머 멱등성 A(자연)/B(인박스) 택1, Outbox↔Inbox
  = effectively-once, 실패 처리 3방식·DLQ⟂멱등성. + phase 5b 실제 적용(feed 자연키 / notification
  파생 dedup_key), 파티션 키 vs 멱등성 키 vs 프로듀서 멱등성 구분.

- [**Redis 피드 캐시 · 이중 쓰기 일관성 (phase 5a)**](docs/learnings/redis-feed-cache-consistency.md)
  진실의 원천=Postgres·Redis=disposable, cache-aside + write-through-if-present + fail-safe evict,
  append vs invalidate(thundering herd), 콜드 캐시 정책, 구현 디테일.

- [**하이브리드 팬아웃 · Kafka Streams follower-count (phase 5c)**](docs/learnings/hybrid-fanout-kafka-streams.md)
  셀럽 읽기 시점 pull, Streams KTable 를 쓴 이유와 오버엔지니어링 경계, 관계 키 upsert/tombstone
  으로 멱등 집계, interactive query 헥사고날 격리, post 닉네임 스냅샷, Streams 통합 테스트 함정.

## 시간순 요약

- **2026-07-03** (phase 4 후속) — Kafka 토픽/파티션/멱등성/Outbox·Inbox/DLQ: 위 앞 두 문서.
- **2026-07-04** (phase 5) — Redis 캐시(5a), 컨슈머 idempotency(5b), 하이브리드 팬아웃·Streams(5c).
- **2026-07-05** (Q&A 심화) — `@Externalized` 문법·파티션 결정 주체·순서 3단 계약·해시 비유(1번 문서),
  파티션 키 vs 멱등성 키 vs 프로듀서 멱등성 구분(2번 문서).
