# 하이브리드 팬아웃 · Kafka Streams follower-count (phase 5c)

[← LEARNINGS 인덱스](../../LEARNINGS.md)

---

## 2026-07-04 — 하이브리드 팬아웃 · Streams 집계

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
  오면 +2 가 되어 카운트가 틀어진다(컨슈머 중복 문제가 집계에서 재현).
- 우리 모델: **관계 키(`follower:followee`)를 키로 하는 KTable**.
  - follow → 그 키에 값 upsert, unfollow → 그 키를 **tombstone(null)** 로 삭제.
  - 그 위에서 `groupBy(followee).count()`. KTable 집계는 키의 이전 값을 알고 감산/가산하므로,
    같은 관계가 재전달돼도 **같은 키 upsert 라 카운트가 두 번 오르지 않고**, tombstone 은 정확히 -1.
- 즉 [자연 멱등성(A)](idempotency-and-delivery.md#2-컨슈머-멱등성-두-방법--택1-섞는-게-아님)을 **스트림 버전**으로
  옮긴 것 — "누적"이 아니라 "관계의 현재 상태"를 KTable 로 두면 멱등이 공짜로 따라온다.
  `TopologyTestDriver` 로 집계·재전달 멱등·tombstone 감산을 결정 검증.

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

### 7. Streams 를 통합 테스트에 넣을 때의 함정 (경험)

- Kafka Streams 를 `@SpringBootTest` 전체 컨텍스트에 넣으면 **application-id/state-dir 충돌·타이밍**
  으로 플래키 위험(캐시된 여러 컨텍스트가 같은 app-id 로 붙으면 파티션·스토어 경합).
- 대응:
  - **토폴로지 자체는 `TopologyTestDriver`** 로 브로커 없이 결정적으로 검증(집계/멱등/tombstone).
  - **E2E 는 격리**: threshold 등 프로퍼티가 다르면 어차피 별도 컨텍스트가 뜨므로, 거기에 **고유
    `spring.kafka.streams.application-id`** + `@DynamicPropertySource` 로 **매 실행 새 임시 state dir**
    를 준다 → 기본 컨텍스트의 Streams 와 충돌 없이 공존.
- Boot 4.1: `@EnableKafkaStreams` + `spring.kafka.streams.application-id` 로 `StreamsBuilder`/
  `StreamsBuilderFactoryBean` 자동설정. `kafka-streams-test-utils` 버전은 Boot 가 관리.
