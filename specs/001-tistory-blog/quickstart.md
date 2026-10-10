# Quickstart: 티스토리형 블로그 서비스

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **API**: [contracts/rest-api.md](./contracts/rest-api.md)

구현이 끝난 뒤 로컬에서 실행하고, P0 흐름이 명세대로 동작하는지 확인하는 안내다. 코드는 `/speckit-tasks` → `/speckit-implement` 단계에서 만들어진다.

## 준비물

- Java 17, Maven 3.9 이상
- Node.js 20 LTS (npm 포함)
- 최신 크롬, 사파리 또는 엣지
- 카카오 개발자 앱 1개: REST API 키와 Client Secret, Redirect URI `http://blogdock.localhost:8080/login/oauth2/code/kakao` 등록

## 설정

`backend/.env.local`(저장소에 올리지 않음) 또는 환경 변수:

| 이름 | 예시 | 설명 |
| --- | --- | --- |
| `KAKAO_CLIENT_ID` | (카카오 REST API 키) | 카카오 로그인 |
| `KAKAO_CLIENT_SECRET` | (카카오 Client Secret) | 카카오 로그인 |
| `APP_DOMAIN` | `blogdock.localhost` | 서비스 도메인. 블로그는 `{주소}.APP_DOMAIN` |
| `ADMIN_SOCIAL_IDS` | `1234567890` | 서비스 관리자로 지정할 카카오 회원번호(쉼표로 여러 개) |
| `UPLOAD_DIR` | `./data/uploads` | 이미지 저장 위치 |

## 실행

```bash
# 터미널 1: 백엔드 (http://localhost:8080, H2 파일 DB는 backend/data/)
cd backend
mvn spring-boot:run

# 터미널 2: 프론트엔드 (http://blogdock.localhost:5173)
cd frontend
npm install
npm run dev
```

브라우저에서 `http://blogdock.localhost:5173`을 연다. 블로그는 `http://{주소}.blogdock.localhost:5173`에서 열린다.

## 자동 테스트

```bash
cd backend && mvn test            # 권한표, 404/403, 중복 요청, 공개 범위 (기능 ID별)
cd frontend && npm test           # 화면 단위 테스트
cd frontend && npm run test:e2e   # P0 한 바퀴 (테스트 프로필의 가짜 로그인 사용)
```

## 수동 검증 시나리오 (P0 한 바퀴)

| 순서 | 할 일 | 기대 결과 | 관련 |
| --- | --- | --- | --- |
| 1 | 비회원으로 홈을 연다 | 공개 글 최신순 목록(처음엔 빈 상태 안내) | HOME-01 |
| 2 | 카카오로 로그인한다 | 로그인 전 화면으로 돌아옴 | AUTH-01 |
| 3 | 주소 `ab`, `-dog`, `admin`을 차례로 입력한다 | 각각 이유와 함께 거절 | BLOG-01, POL-02 |
| 4 | 주소 `my-dog`, 이름 "보리네 산책일기"로 개설한다 | `my-dog.blogdock.localhost:5173`으로 이동, 로그인 유지 | BLOG-01, POL-02-5 |
| 5 | 카테고리 "산책"을 만들고, 이미지 2장과 코드 블록이 든 글을 태그 `강아지`, `산책`으로 발행한다 | 글 상세로 이동, 공감·댓글·조회수 0, 주소 `/entry/{제목}` | POST-01, POST-05, CAT-01, TAG-01 |
| 6 | 제목을 비우고 발행해 본다 | 제목 오류만 표시, 입력 내용 유지 | POST-01-2, COM-02-2 |
| 7 | 5번 글의 제목을 고친다 | 주소·발행일·목록 순서 그대로 | POST-02 |
| 8 | 다른 카카오 계정(또는 시크릿 창 비회원)으로 홈 → 글 상세 → 카테고리 → 태그 → 블로그 내 검색(`  강아지  `)을 해 본다 | 모두 같은 사이드바, 검색어 앞뒤 공백 무시 | BLOG-04, CAT-02, TAG-02, SRCH-01 |
| 9 | 두 번째 계정으로 댓글과 공감을 남기고, 공감 버튼을 빠르게 여러 번 누른다 | 댓글 1개, 공감 상태 정확, 새로고침 후 개수 일치 | CMT-01, SOC-01, NFR-08·09 |
| 10 | 첫 계정으로 그 댓글을 지운다 | 지워짐, 수정 버튼은 없음 | CMT-02 |
| 11 | 글을 비공개로 바꾸고 두 번째 계정으로 같은 주소를 연다 | 404, 홈·검색·사이드바 개수에서도 사라짐 | POST-06, POL-01 |
| 12 | 두 번째 계정으로 `/admin`과 첫 계정 글의 수정 API를 직접 호출한다 | 각각 403 | ADMIN-01, COM-01 |
| 13 | 제목에 `<script>alert(1)</script>`를 넣은 글을 발행한다 | 문자 그대로 보이고 실행되지 않음 | NFR-01 |
| 14 | 휴대폰 폭(360px)으로 1~11을 다시 해 본다 | 가로 스크롤 없이 모두 가능 | NFR-13 |

모든 순서가 기대 결과대로면 P0(SC-002, SC-003 일부)가 완료된 것이다.
