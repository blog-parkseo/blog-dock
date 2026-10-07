# Research: 티스토리형 블로그 서비스

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-07

기술 결정과 그 이유를 모은 문서다. 출발점은 사용자 맥에 있는 프로토타입 「보리네 산책일기」(`bori-blog/blog-react`: React 18 + Vite 5, `bori-blog/blog-backend`: Java 17 + Spring Boot 3.3 + JPA + H2)다. 같은 스택을 이어 쓰면 이미 익숙한 도구로 바로 시작할 수 있다.

## R1. 저장소 구조

- **Decision**: 한 저장소에 `backend/`(Spring Boot)와 `frontend/`(React)를 둔다.
- **Rationale**: 명세·계획·작업이 한곳에 있어 기능 ID로 추적하기 쉽다. 프로토타입도 같은 두 폴더 구조다.
- **Alternatives**: 저장소 두 개로 분리 → 명세와 코드가 흩어지고 PR이 두 번 필요해 기각.

## R2. 백엔드

- **Decision**: Java 17, Spring Boot 3.3.x, Maven. Spring Web, Spring Data JPA, Validation, Spring Security, OAuth2 Client, Flyway.
- **Rationale**: 프로토타입과 같은 버전. Spring Security가 세션, CSRF, 권한 검사를 서버에서 일관되게 처리한다(헌법 IV).
- **Alternatives**: 프로토타입의 Node 로그인 서버(`server/`) → 이메일 로그인용이고 카카오 로그인 결정과 맞지 않아 기각.

## R3. 프론트엔드

- **Decision**: React 18 + Vite 5 + React Router 6. 서버 상태는 TanStack Query로 다룬다.
- **Rationale**: 프로토타입과 같은 스택. TanStack Query의 낙관적 업데이트로 공감·구독 버튼이 즉시 바뀌고(NFR-07), 실패하면 원래대로 되돌린다.
- **Alternatives**: Next.js → 서버 렌더링이 필요한 요구사항이 없고 배울 것이 늘어 기각.

## R4. 로그인 (선택 항목: 카카오)

- **Decision**: Spring Security OAuth2 Client에 카카오를 provider로 등록한다. 로그인 후 서버 세션(HttpOnly, SameSite=Lax 쿠키)을 쓴다.
- **Rationale**: 토큰을 브라우저 저장소에 두지 않아 로그인 정보가 화면·주소창에 드러나지 않는다(NFR-04). 서버 세션은 이용 제한 회원을 다음 요청부터 바로 막기 쉽다(ADMIN-02).
- **Alternatives**: JWT를 프론트에 저장 → 즉시 무효화가 어렵고 XSS에 취약해 기각.
- **비고**: 카카오 개발자 콘솔에 Redirect URI `http://blogdock.localhost:8080/login/oauth2/code/kakao`(개발용)를 등록해야 한다. 키는 환경 변수 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`으로 받고 저장소에 넣지 않는다.

## R5. 블로그 서브도메인 (선택 항목: 서브도메인)

- **Decision**: 서비스 주소를 `blogdock.localhost`(개발), 블로그는 `{주소}.blogdock.localhost`로 연다. 프론트는 `window.location.hostname`에서 블로그 주소를 읽고, API는 모든 화면에서 같은 출처의 `/api`로 부른다. 세션 쿠키는 `Domain=blogdock.localhost`로 발급해 블로그를 오가도 로그인이 유지된다(POL-02-5).
- **Rationale**: `*.localhost`는 최신 브라우저가 별도 DNS 설정 없이 127.0.0.1로 연결한다. 운영에서는 와일드카드 DNS(`*.도메인`)와 리버스 프록시 한 대로 같은 구조를 쓴다.
- **Alternatives**: 경로 방식(`/my-dog`) → 팀 선택 항목에서 서브도메인을 골랐으므로 기각. 
- **위험과 대안**: 일부 브라우저가 `.localhost` 하위 도메인의 공유 쿠키를 다르게 다루면, 개발용 도메인을 `lvh.me`(공개 DNS가 127.0.0.1을 돌려줌)로 바꾼다. 구현 첫 작업에서 크롬·사파리·엣지로 확인한다.

## R6. 에디터 (선택 항목: 혼합)

- **Decision**: Toast UI Editor(마크다운 탭 + WYSIWYG 탭)를 쓰고, 원문은 마크다운으로 저장한다. 서버가 commonmark-java로 HTML을 만들고 OWASP Java HTML Sanitizer로 허용 태그만 남긴 HTML과 검색용 평문을 함께 저장한다.
- **Rationale**: 한 에디터에서 두 방식을 오가며 POST-01-4의 표현 요소를 모두 지원한다. 정화를 서버에서 하므로 요청을 직접 보내도 스크립트가 저장되지 않는다(NFR-01). 다시 수정할 때는 마크다운 원문을 불러오므로 서식이 남는다.
- **Alternatives**: HTML 원문 저장 → 정화 누락 위험이 커서 기각. 클라이언트에서만 정화(DOMPurify) → 직접 요청을 막지 못해 기각(표시 단계의 2차 방어로는 함께 쓴다).

## R7. 글 주소 (선택 항목: 제목 기반)

- **Decision**: 처음 발행할 때 제목으로 slug를 만든다. 앞뒤 공백 제거 → 공백을 `-`로 → 한글·영문·숫자·`-` 외 문자 제거 → 최대 80자. 같은 블로그에 이미 있으면 `-2`, `-3`을 붙인다. 주소는 `{블로그}.도메인/entry/{slug}`이고 한글은 URL 인코딩으로 열린다. 제목만 공백·특수문자면 `post-{id}`를 쓴다.
- **Rationale**: POL-03-1·2(불변, 중복 없음, 한글 지원)를 만족한다. DB에 `(blog_id, slug)` 유니크 제약을 둔다.

## R8. 데이터베이스

- **Decision**: 개발·테스트는 H2 파일 DB(MySQL 호환 모드), 배포는 MySQL 8. 스키마는 Flyway 마이그레이션으로 관리한다. 시각은 UTC(`Instant`)로 저장하고 화면에서 Asia/Seoul로 보여 준다(NFR-15).
- **Rationale**: 프로토타입이 H2를 쓰고 있어 설치 없이 시작할 수 있다. Flyway로 두 DB에 같은 스키마를 적용한다.
- **Alternatives**: PostgreSQL → 기능상 차이가 작고, 팀 환경에서 MySQL이 더 익숙하다고 가정했다. 바꾸려면 이 항목만 고치면 된다.

## R9. 검색

- **Decision**: 블로그 내 검색과 통합 검색 모두 DB 질의로 한다. 제목·평문 본문·태그 이름을 `LOWER(...) LIKE LOWER(:q)`로 찾는다.
- **Rationale**: 학습용 규모(블로그 수백, 글 수천)에서 충분하다. 대소문자 무시(SRCH-01-1)를 간단히 만족한다.
- **Alternatives**: Elasticsearch, MySQL FULLTEXT(한글 형태소 문제) → 규모 대비 과해서 기각(헌법 VI).

## R10. 목록과 페이지네이션

- **Decision**: 블로그 글 목록·카테고리·태그·검색은 페이지 번호 방식(기본 10개). 홈 최신 글·피드는 커서 방식(`published_at`, `id` 쌍)으로 더 불러온다.
- **Rationale**: 커서 방식은 새 글이 올라와도 중복·누락이 없다(POL-04-3). 정렬은 `published_at DESC, id DESC`로 하면 "같은 시각이면 나중에 만든 글이 위"(POL-04-1)가 된다.

## R11. 중복 요청 방지 (NFR-08)

- **Decision**: 글 발행·댓글 등록은 프론트가 만든 `Idempotency-Key` 헤더(UUID)를 받아 회원별로 24시간 저장하고, 같은 키가 오면 처음 결과를 돌려준다. 공감·구독·저장은 `PUT`(켜기)/`DELETE`(끄기)와 DB 복합 PK로 여러 번 와도 결과가 하나다.
- **Rationale**: 버튼 비활성화만으로는 직접 요청을 막지 못한다. 개수는 저장하지 않고 COUNT로 계산해 늘 실제 값과 같다(NFR-09).

## R12. 이미지 업로드

- **Decision**: 파일 앞부분의 매직 바이트로 jpg·png·gif·webp를 판별한다(확장자 무시). 크기 상한 10MB. 사진은 metadata-extractor로 EXIF 방향을 읽어 바로 세운 뒤 Thumbnailator로 목록용 작은 이미지(가로 400px)를 만든다. GIF는 원본 그대로 둔다. 저장은 로컬 디스크(`UPLOAD_DIR`)에 하고 `/files/**`로 내보낸다.
- **Rationale**: POL-06과 NFR-05·06을 만족하는 가장 단순한 방식. 저장 위치는 인터페이스 뒤에 두어 나중에 오브젝트 스토리지로 바꿀 수 있다.

## R13. 조회수와 인기 글

- **Decision**: 조회는 `post_view_log(post_id, viewer_key, viewed_at)`에 남기고, 같은 viewer_key(회원 id 또는 비회원 쿠키의 해시)가 30분 안에 다시 열면 세지 않는다. 인기 글은 최근 7일 공감 수 + 댓글 수 합계로 매기고 결과를 10분간 캐시(Caffeine)한다.
- **Rationale**: 명세의 임시 결정(짧은 시간 재방문 제외)과 선택 항목(7일 공감+댓글)을 그대로 따른다. 갱신 주기 10분은 "기준을 설계 문서에 밝힌다"는 요구를 채운다.

## R14. 예약 발행과 이용 제한 해제

- **Decision**: Spring `@Scheduled`로 1분마다 예약 시각이 지난 글을 공개한다. 이용 제한은 별도 해제 작업 없이 요청마다 `starts_at <= now < ends_at`인지 확인한다.
- **Rationale**: 단일 서버 기준 가장 단순하다. 공개 시각은 실제 공개한 시각으로 기록한다(POST-13).

## R15. 테스트

- **Decision**: 백엔드는 JUnit 5 + Spring Boot Test + MockMvc(권한표·404/403·중복 요청 검사 포함). 프론트는 Vitest + React Testing Library. P0 한 바퀴 흐름은 Playwright로 E2E 테스트한다(카카오 로그인은 테스트 프로필에서 가짜 로그인으로 대체).
- **Rationale**: 수락 시나리오를 그대로 테스트로 옮길 수 있다. 권한은 화면과 무관하게 API에서 검사해야 하므로 MockMvc 테스트가 핵심이다(SC-003).

## R16. 각자 정하는 값 (명세 Assumptions에서 미룬 것)

| 항목 | 값 |
| --- | --- |
| 업로드 크기 상한 | 10MB |
| 로그인 유지 기간 | 14일(활동하면 연장) |
| 조회수 중복 판단 시간 | 30분 |
| 인기 글 갱신 주기 | 10분 |
| 추천 블로그 기준 (P2) | 같은 주제 블로그 중 구독자 수 순 |
| 인기 블로거 기준 (P2) | 최근 7일 블로그 전체 공감+댓글 합계 |
| 방문자 집계 (P2) | 같은 방문자는 하루 한 번만 센다 |
| 성능 목표 | 목록·글 상세 API p95 300ms 이하(개발 PC, 글 1만 개 기준) |
