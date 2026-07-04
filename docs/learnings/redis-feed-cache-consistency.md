# Redis 피드 캐시 · 이중 쓰기 일관성 전략 (phase 5a)

[← LEARNINGS 인덱스](../../LEARNINGS.md)

---

## 2026-07-04 — 피드 캐시 일관성

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
  커넥션은 `spring-boot-starter-data-redis` 자동설정 + `spring.data.redis.host/port`(컨테이너는
  `SPRING_DATA_REDIS_HOST=redis` 로 오버라이드)로 끝 — 명시적 `RedisConnectionFactory` 빈 없음.
  테스트는 `GenericContainer("redis:7-alpine")` + `@ServiceConnection(name="redis")`(이미지명으로
  Boot 의 Redis ConnectionDetailsFactory 가 매칭 → `spring.data.redis.*` 자동 주입).
