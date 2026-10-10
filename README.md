# blog-dock

티스토리형 블로그 서비스. [GitHub Spec Kit](https://github.com/github/spec-kit)으로 "명세 → 계획 → 작업 → 구현" 순서로 만든다.

## 지금 만들어진 것 (1차 구현, 기능 35개)

요구사항 명세서의 1차 범위(P0 24개 + P1 11개)와 1차 ERD(테이블 11개)대로 만들었다.

- 회원: 카카오 로그인(키를 넣으면 켜짐), 개발용 닉네임 로그인, 로그아웃, 14일 로그인 유지, 회원정보 수정
- 블로그: 개설(주소 규칙·예약어 검사), 정보·프로필 이미지 수정, 사이드바(카테고리별 글 수)
- 글: 마크다운·보이는 대로 편집 에디터, 이미지 업로드(10MB, 썸네일), 대표 이미지, 공개·비공개, 임시저장, 수정·삭제, 이전·다음 글, 주소 복사
- 카테고리(추가·이름 변경·삭제·순서), 태그(10개까지, 태그별 목록, 태그 모아 보기), 블로그 내 검색
- 댓글(쓰기·수정·삭제, 글 주인의 삭제), 공감, 홈 최신 글(더보기), 내 글 관리, 관리자 영역
- 보안: 본문 스크립트 제거, CSRF 방어, 연달아 눌러도 한 번만 처리(요청 키), 권한 검사(401·403·404)
- 블로그 주소를 `주소.blogdock.localhost` 서브도메인으로도 열 수 있다(선택)

## 실행하기

Java 17 이상과 Node 20 이상이 필요하다. 터미널 두 개를 연다.

```bash
# 터미널 1: 백엔드 (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 터미널 2: 화면 (http://localhost:5173)
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 연다. 로그인 화면의 "개발용 로그인"에 아무 닉네임이나 넣으면 가입된다. `admin`으로 들어가면 관리자다.

- DB는 `backend/data/` 폴더의 H2 파일이다. 처음부터 다시 하고 싶으면 백엔드를 끄고 이 폴더를 지운다.
- 올린 이미지는 `backend/uploads/`에 저장된다.
- 백엔드 테스트: `cd backend && ./mvnw test` (18개)

### 카카오 로그인 켜기

1. [카카오 개발자](https://developers.kakao.com)에서 앱을 만들고 "카카오 로그인"을 켠다. 동의 항목에서 닉네임과 프로필 사진을 고른다.
2. Redirect URI에 `http://localhost:5173/login/oauth2/code/kakao`를 넣는다.
3. 키를 환경 변수로 넣고 kakao 프로필로 실행한다. **키는 파일에 적거나 GitHub에 올리지 않는다.**

```bash
cd backend
KAKAO_CLIENT_ID=REST_API_키 KAKAO_CLIENT_SECRET=시크릿_키 SPRING_PROFILES_ACTIVE=kakao ./mvnw spring-boot:run
```

시크릿 키를 쓰지 않게 설정했다면 `KAKAO_CLIENT_SECRET`은 빼도 된다. 켜지면 로그인 화면에 "카카오로 시작하기" 버튼이 나온다.

### 블로그 주소를 서브도메인으로 열기 (선택)

```bash
# 백엔드
SPRING_PROFILES_ACTIVE=subdomain ./mvnw spring-boot:run      # 카카오도 쓰면 kakao,subdomain
# 화면
VITE_BLOG_DOMAIN=blogdock.localhost npm run dev
```

`http://blogdock.localhost:5173`으로 들어가면 블로그가 `http://주소.blogdock.localhost:5173`으로 열리고, 블로그를 오가도 로그인이 유지된다. 크롬·엣지는 `*.localhost`를 따로 설정하지 않아도 내 컴퓨터로 연결한다. 카카오를 같이 쓰면 Redirect URI를 `http://blogdock.localhost:5173/login/oauth2/code/kakao`로 넣는다.

### 운영 배포 때

`SPRING_PROFILES_ACTIVE=prod,kakao`로 실행하면 개발용 로그인이 꺼지고 MySQL을 쓴다. `DB_ADDRESS`, `DB_PORT`(기본 3306), `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `UPLOAD_DIR`을 환경 변수로 넣는다(`backend/src/main/resources/application-prod.yml`).

### 학교 서버에 자동 배포

`.github/workflows/deploy.yml`이 main에 푸시하거나 머지할 때마다 화면과 서버를 jar 하나로 빌드하고 `Dockerfile`로 도커 이미지를 만든다. 이미지를 학교 서버의 `~/blog-dock`에 올리고 `deploy/deploy.sh`가 SSH로 접속해 `blog-dock` 컨테이너를 다시 띄운다 (서버 네트워크를 같이 써서 `APP_PORT`로 바로 연다).

1. 저장소 Settings → Secrets and variables → Actions의 **Secrets**에 `DB_ADDRESS`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SSH_ADDRESS`, `SSH_ID`, `SSH_PASSWORD`, `SSH_PORT`를 넣는다.
2. 같은 화면의 **Variables** 탭에 블로그 포트 `APP_PORT`를 넣는다. 이 값이 없으면 배포하지 않는다.
3. 코드를 올리면 Actions 탭에서 진행 상황을 볼 수 있다. 서버에서 로그는 `docker logs blog-dock`, 손으로 다시 띄우려면 `~/blog-dock/deploy.sh`.

## 어디에 무엇이 있나

| 위치 | 내용 |
| --- | --- |
| `backend/` | Spring Boot 3.5 (Java 17). `src/main/resources/db/migration/V1__init.sql`이 1차 ERD의 테이블 11개 |
| `frontend/` | React 19 + Vite |
| [`specs/001-tistory-blog/spec.md`](specs/001-tistory-blog/spec.md) | 기능명세서 (71개 기능, 기능 ID는 팀 통합 명세서와 같음) |
| [`specs/001-tistory-blog/plan.md`](specs/001-tistory-blog/plan.md) | 설계 계획 (기술 스택, 폴더 구조, 구현 단계). 자세한 내용은 같은 폴더의 research, data-model, contracts, quickstart |
| [`specs/001-tistory-blog/tasks.md`](specs/001-tistory-blog/tasks.md) | 작업 목록 133개 (기반 → P0 → P1 → P2 순서, 작업마다 기능 ID) |
| [`specs/001-tistory-blog/checklists/requirements.md`](specs/001-tistory-blog/checklists/requirements.md) | 명세 품질 체크리스트 |
| [`.specify/memory/constitution.md`](.specify/memory/constitution.md) | 프로젝트 원칙 (모든 단계에서 지킬 규칙) |
| `.specify/templates/`, `.specify/scripts/` | Spec Kit 템플릿과 스크립트 |
| `.claude/skills/speckit-*` | Claude Code에서 쓰는 Spec Kit 명령 |

## 다음 단계 (Claude Code에서 순서대로 입력)

1. `/speckit-clarify` — 명세에서 애매한 부분을 질문받고 답을 반영한다 (선택)
2. ~~`/speckit-plan`~~ — 완료 (React + Vite, Spring Boot 3, MySQL/H2)
3. ~~`/speckit-tasks`~~ — 완료 (작업 133개)
4. `/speckit-implement` — 작업 목록 순서대로 구현한다

명세를 고치고 싶으면 `spec.md`를 직접 고치거나 `/speckit-specify`로 다시 써도 된다.
