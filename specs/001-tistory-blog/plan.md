# Implementation Plan: 티스토리형 블로그 서비스

**Branch**: `001-tistory-blog` | **Date**: 2026-10-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-tistory-blog/spec.md`

## Summary

티스토리처럼 회원이 자기 블로그를 열어 글을 쓰고, 다른 사람이 홈에서 발견해 읽고 댓글·공감·구독하는 블로그 플랫폼을 만든다(기능 71개, P0 24개 우선).
기술은 사용자의 프로토타입 「보리네 산책일기」와 같은 React 18 + Vite(프론트)와 Java 17 + Spring Boot 3.3(백엔드)을 쓴다. 카카오 로그인은 Spring Security OAuth2와 서버 세션으로, 블로그 서브도메인은 `{주소}.blogdock.localhost`와 공유 세션 쿠키로, 혼합 에디터는 Toast UI Editor + 서버 측 마크다운 렌더링·정화로 구현한다. 자세한 결정은 [research.md](./research.md)에 있다.

## Technical Context

**Language/Version**: Java 17 (백엔드), JavaScript ES2022 + JSX (프론트엔드, Node 20 LTS로 빌드)

**Primary Dependencies**: Spring Boot 3.3 (Web, Data JPA, Validation, Security, OAuth2 Client), Flyway, commonmark-java, OWASP Java HTML Sanitizer, Thumbnailator, metadata-extractor, Caffeine / React 18, Vite 5, React Router 6, TanStack Query 5, Toast UI Editor 3, DOMPurify

**Storage**: H2 파일 DB(개발·테스트, MySQL 호환 모드), MySQL 8(배포). 이미지는 로컬 디스크(`UPLOAD_DIR`)

**Testing**: JUnit 5, Spring Boot Test, MockMvc / Vitest, React Testing Library / Playwright(E2E)

**Target Platform**: 리눅스 서버 1대 + 리버스 프록시(와일드카드 서브도메인), 최신 크롬·사파리·엣지(데스크톱·휴대폰)

**Project Type**: 웹 애플리케이션 (backend + frontend)

**Performance Goals**: 목록·글 상세 API p95 300ms 이하(글 1만 개 기준), 화면은 2초 안에 내용 표시 시작(SC-006)

**Constraints**: 한국 시간 표시, 360px 화면에서 가로 스크롤 없음, 사용자 입력 스크립트 실행 금지, 모든 권한은 서버에서 검사

**Scale/Scope**: 학습용 규모. 회원·블로그 수백, 글 수천~1만, 화면 약 20개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 확인 내용 | 결과 |
| --- | --- | --- |
| I. 사용자가 확인할 수 있는 규칙 | spec.md에는 동작 규칙만 있고, 기술 결정은 이 plan과 research.md에만 있다 | ✅ |
| II. 기능 ID 추적성 | data-model.md와 contracts의 모든 항목에 관련 기능 ID를 붙였다. tasks.md도 같은 ID를 쓴다 | ✅ |
| III. P0 우선 | 구현 단계를 P0(Story 1~5) → P1(Story 6~9) → P2(Story 10) 순으로 나눴다 | ✅ |
| IV. 서버에서 지키는 권한 | 권한·공개 범위 판단을 백엔드 한곳(`PostAccessPolicy`, Spring Security)에서 하고, 남의 비공개 글은 404로 응답한다. 본문은 서버에서 정화한다 | ✅ |
| V. 신뢰할 수 있는 데이터 | Idempotency-Key, 복합 PK, COUNT 계산, 트랜잭션 삭제로 중복·불일치·일부 처리를 막는다 | ✅ |
| VI. 단순함 | 서버 1대, DB 1개, 외부 검색엔진·메시지 큐 없음. Complexity Tracking 항목 없음 | ✅ |

**Phase 1 설계 후 재확인**: data-model.md의 엔티티와 contracts의 API가 위 판단을 바꾸지 않는다. ✅

## Project Structure

### Documentation (this feature)

```text
specs/001-tistory-blog/
├── spec.md              # 기능명세서
├── plan.md              # 이 문서
├── research.md          # 기술 결정 (Phase 0)
├── data-model.md        # 데이터 모델 (Phase 1)
├── quickstart.md        # 실행·검증 안내 (Phase 1)
├── contracts/
│   └── rest-api.md      # REST API 계약 (Phase 1)
├── checklists/
│   └── requirements.md  # 명세 품질 체크리스트
└── tasks.md             # 작업 목록 (/speckit-tasks에서 생성)
```

### Source Code (repository root)

```text
backend/                         # Spring Boot (Java 17, Maven)
├── pom.xml
└── src/
    ├── main/java/com/blogdock/
    │   ├── config/              # Security, 서브도메인 해석, 웹 설정, 예외 처리(COM-02)
    │   ├── auth/                # AUTH: 카카오 로그인, 세션, 회원
    │   ├── blog/                # BLOG: 블로그, 주소 검증, 예약어
    │   ├── post/                # POST: 글, slug, 공개 범위 판단, 조회수, 예약 발행
    │   ├── category/            # CAT
    │   ├── tag/                 # TAG
    │   ├── comment/             # CMT: 댓글, 방명록
    │   ├── reaction/            # SOC: 공감, 저장
    │   ├── subscription/        # SUB: 구독, 피드, 알림
    │   ├── search/              # SRCH
    │   ├── home/                # HOME: 최신 글, 인기 글, 주제별
    │   ├── manage/              # MNG
    │   ├── admin/               # ADMIN: 제재, 신고, 공지, 이력
    │   ├── image/               # 업로드, 썸네일
    │   └── common/              # Idempotency, 페이지·커서, 시간
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/        # Flyway V1__init.sql ...
    └── test/java/com/blogdock/  # 기능 ID별 MockMvc·서비스 테스트

frontend/                        # React 18 + Vite 5
├── package.json
├── vite.config.js               # /api, /files → localhost:8080 프록시
├── src/
│   ├── main.jsx
│   ├── app/                     # 라우터: 서비스 화면 vs 블로그 화면(호스트로 구분)
│   ├── api/                     # API 클라이언트, TanStack Query 훅
│   ├── pages/                   # 홈, 검색, 피드, 블로그 메인, 글 상세, 글쓰기, 관리, 관리자
│   ├── components/              # 사이드바, 글 카드, 댓글, 공감·구독 버튼, 에디터 래퍼
│   └── lib/                     # 시간 표시(KST), 정화(DOMPurify), 입력 검증
└── tests/
    ├── unit/                    # Vitest
    └── e2e/                     # Playwright: P0 한 바퀴
```

**Structure Decision**: 프론트엔드와 백엔드가 분리된 웹 애플리케이션 구조를 쓴다. 백엔드는 영역코드별 패키지로 나눠 기능 ID와 코드 위치가 바로 대응되게 한다. 개발 중에는 Vite 개발 서버(5173)가 `*.blogdock.localhost` 요청을 받고 `/api`, `/files`, `/oauth2`, `/login/oauth2`를 백엔드(8080)로 넘긴다.

## 구현 단계

| 단계 | 사용자 스토리 | 기능 | 완료 기준 |
| --- | --- | --- | --- |
| 0. 기반 | — | 프로젝트 골격, Flyway 초기 스키마, Security·세션·서브도메인, 예외 처리, 테스트 환경 | 빈 화면과 `/api/health`가 서브도메인에서 열리고, 공유 쿠키가 동작한다 |
| 1. P0 | Story 1~5 | AUTH-01·02, BLOG-01~04, POST-01~06, CAT-01·02, TAG-01·02, CMT-01·02, SOC-01, SRCH-01, HOME-01, ADMIN-01, COM-01·02 | P0 한 바퀴 E2E 테스트와 권한표 MockMvc 테스트가 통과한다(SC-002, SC-003) |
| 2. P1 | Story 6~9 | AUTH-03~05, POST-07~11, CAT-03·04, TAG-03, CMT-03·04, SOC-02, SUB-01~03, SRCH-02, HOME-02·03, MNG-01·02, ADMIN-02·03 | 각 스토리 수락 시나리오 테스트 통과 |
| 3. P2 | Story 10 | 기능 하나씩 독립 추가 (BLOG-06·08 제외) | 기능별 규칙 테스트 통과 |

## Complexity Tracking

헌법 위반 항목이 없어 비워 둔다.
