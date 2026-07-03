# philstargram front

philstargram 백엔드를 눈으로 확인하기 위한 얇은(thin) React 데모. 인증이 없어
**로그인 대신 상단에서 현재 사용자 ID 를 직접 전환**하며 사용한다.

## 스택

React 18 + Vite + TypeScript, 순수 CSS. 상태관리/라우터 없음(의도적으로 최소화).

## 실행

### 전체 스택 (운영 형태, 한 번에)

```bash
# 레포 루트에서
docker compose up -d --build
```

- 프론트: **http://localhost:3000** (nginx 가 정적 파일 서빙 + `/api` 를 백엔드로 프록시)
- 백엔드: http://localhost:8080, DB: localhost:5432

### 개발 (핫리로드)

```bash
# 레포 루트: DB 만 컨테이너 + 백엔드 로컬 실행
docker compose up -d postgres
./gradlew.bat bootRun               # http://localhost:8080

# front/ 에서
npm install
npm run dev                         # http://localhost:5173
```

프론트는 API 를 **상대 경로(`/api/...`)**로 호출한다. 개발에선 Vite 프록시(`vite.config.ts`),
운영에선 nginx(`nginx.conf`) 가 백엔드로 넘긴다 — 둘 다 프론트와 같은 오리진이라 **CORS 가 필요 없다.**

## 사용 흐름 (예)

1. **회원 가입** 으로 사용자 A 생성 → 자동으로 현재 사용자로 설정됨.
2. 다시 **회원 가입** 으로 사용자 B 생성(현재 사용자가 B 로 바뀜).
3. 상단에서 현재 사용자를 **A** 로 전환 → **팔로우** 에 B 의 id 입력.
4. 현재 사용자를 **B** 로 전환 → **게시글 작성**.
5. 현재 사용자를 **A** 로 전환 → **내 피드 새로고침** → 잠시 뒤 B 의 글이 보임.
6. B 로 전환 → **내 알림** → A 의 팔로우 알림 확인.

> 피드/알림은 이벤트(Outbox → 비동기 리스너) 기반이라 즉시 반영되지 않는다.
> 새로고침 버튼으로 다시 조회한다.

## API 계약 (백엔드)

| 동작 | 메서드 · 경로 |
| --- | --- |
| 회원 가입 | `POST /api/members` |
| 회원 조회 | `GET /api/members/{id}` |
| 게시글 작성 | `POST /api/posts` |
| 게시글 조회 | `GET /api/posts/{id}` |
| 팔로우 | `POST /api/members/{followerId}/follow/{followeeId}` |
| 언팔로우 | `DELETE /api/members/{followerId}/follow/{followeeId}` |
| 내 피드 | `GET /api/members/{memberId}/feed` |
| 내 알림 | `GET /api/members/{memberId}/notifications` |

응답은 공통 봉투 `{ success, data, error }` 이며, 클라이언트(`src/api.ts`)가 `data` 만 벗겨서 반환한다.
