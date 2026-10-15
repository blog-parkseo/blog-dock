# blog-dock

티스토리형 블로그 서비스. [GitHub Spec Kit](https://github.com/github/spec-kit)으로 "명세 → 계획 → 작업 → 구현" 순서로 만든다.

이 저장소는 **코드**만 둔다. 기능명세서·설계·작업 목록·ERD 같은 **문서는 [blog-dock-docs](https://github.com/blog-parkseo/blog-dock-docs)** 에 있다.

## 지금 만들어진 것 (1차 구현, 기능 35개)

요구사항 명세서의 1차 범위(P0 24개 + P1 11개)와 1차 ERD(테이블 11개)대로 만들었다.

- 회원: 아이디·비밀번호 가입·로그인(BCrypt 저장, 5번 틀리면 5분 잠금), 카카오 로그인(키를 넣으면 켜짐), 연습용 닉네임 로그인, 로그아웃, 14일 로그인 유지, 회원정보 수정
- 블로그: 개설(주소 규칙·예약어 검사), 정보·프로필 이미지 수정, 사이드바(카테고리별 글 수)
- 글: 마크다운·보이는 대로 편집 에디터, 이미지 업로드(10MB, 썸네일), 대표 이미지, 공개·비공개, 임시저장, 수정·삭제, 이전·다음 글, 주소 복사
- 카테고리(추가·이름 변경·삭제·순서), 태그(10개까지, 태그별 목록, 태그 모아 보기), 블로그 내 검색
- 댓글(쓰기·수정·삭제, 글 주인의 삭제), 공감, 홈 최신 글(더보기), 내 글 관리, 관리자 영역
- 보안: 본문 스크립트 제거, CSRF 방어, 연달아 눌러도 한 번만 처리(요청 키), 권한 검사(401·403·404)
- 블로그 주소를 `주소.blogdock.localhost` 서브도메인으로도 열 수 있다(선택)

2차로 구독을 더했다(테이블 `subscription`, 마이그레이션 `V3__subscription.sql`).

- 구독·해제: 블로그 위쪽과 글 아래의 "+ 구독" 버튼, 구독자 수. 내 블로그는 구독할 수 없고, 비회원은 로그인으로 간다
- 구독 피드: 맨 위 메뉴 "구독 피드"에서 구독한 블로그 목록과 그 블로그들의 공개 글을 최신순으로(더보기)
- 인기 글(홈 맨 위): 최근 7일 동안 받은 공감 수 + 댓글 수(글쓴이 본인 댓글 제외)가 큰 순서로 10개. 서버가 뜰 때와 10분마다 다시 계산해 메모리에 들고 있다. 보여 주기 직전에 공개 글인지 다시 확인한다
- 관리 화면 탭에 공개·비공개·임시저장 글 수

3차로 맞구독 표시, 구독 피드의 추천 블로그, 구독자 공개 글을 더했다.

- 구독자 공개 글(마이그레이션 `V6__post_visibility_subscribers.sql`): 글쓰기의 공개 범위에서 "구독자 공개"를 고르면 주인과 구독자만 본문을 본다. 구독자에게는 블로그 목록과 구독 피드에 "🔒 구독자 전용" 표시로 섞여 보이고, 그 밖의 사람에게는 목록·검색·홈·인기 글·사이드바 개수에서 빠진다. 주소로 직접 열면 제목과 구독 버튼만 있는 잠긴 화면이 나온다. 글에 넣은 사진 주소는 공개라 사진 주소를 아는 사람은 열 수 있다
- 알림(마이그레이션 `V7__notification.sql`): 맨 위 메뉴 맨 앞의 종 아이콘에 읽지 않은 알림 수(9개 넘으면 9+). 내 글의 댓글·공감, 내 블로그 구독, 구독한 블로그의 새 글(처음 발행할 때 한 번, 비공개 글 제외)을 알린다. 내 행동은 알리지 않고, 공감은 같은 사람·같은 글이면 24시간에 한 번. 알림은 댓글·공감 등이 저장된 뒤에 만들어서 실패해도 원래 일에는 영향이 없다. 읽지 않은 수는 60초마다 묻고 다른 탭에 있으면 묻지 않는다. 지워졌거나 볼 수 없게 된 글은 "볼 수 없는 글이에요". 90일 지난 알림은 매일 새벽 4시(한국 시간)에 지운다
- 테마 피드(마이그레이션 `V8__theme.sql`): 사이트 공통 테마 다섯 개(일상·여행·맛집·IT·책·영화). 글쓰기에서 테마를 고르고(선택), 카테고리마다 기본 테마를 정해 두면 그 카테고리를 고를 때 테마가 채워진다. 홈의 최신 글 아래에 글이 있는 테마만 버튼으로 보여 주고, 고른 테마의 소개와 공개 글을 5개씩(공감·댓글 수, "1일 전") 보여 준다. 글 화면의 테마 표시를 누르면 홈의 그 테마로 간다. 테마 목록은 `Theme.java`와 V8의 CHECK에서 같이 고친다
- 예약 맞구독(마이그레이션 `V9__subscription_reservation.sql`): 구독 알림을 보낸 사람이 아직 블로그가 없으면 알림 옆 "+ 맞구독 예약"을 누른다. 그 사람이 블로그를 만드는 순간 자동으로 구독되고, 두 사람 모두 알림을 받는다(🤝 "…님이 블로그를 만들어서 맞구독이 됐어요"). "✓ 맞구독 예약됨"을 누르면 예약을 취소한다. 블로그가 있는 사람이면 같은 자리에 "+ 맞구독하기"가 나온다

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

브라우저에서 `http://localhost:5173`을 연다. 회원가입 화면에서 아이디·비밀번호로 가입하거나, 로그인 화면 아래의 "연습용 닉네임 로그인"에 아무 닉네임이나 넣으면 된다. 관리자는 아이디 `admin`, 비밀번호 `admin1234`(내 맥에서만)다.

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

`SPRING_PROFILES_ACTIVE=prod,kakao`로 실행하면 MySQL을 쓴다. 아이디·비밀번호로 로그인하고, 닉네임 로그인은 꺼져 있다(켜려면 `DEV_LOGIN=true`). 관리자(`admin`) 비밀번호는 `ADMIN_PASSWORD`로 넣는다(없으면 관리자 로그인 불가). `DB_ADDRESS`, `DB_PORT`(기본 3306), `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `UPLOAD_DIR`을 환경 변수로 넣는다(`backend/src/main/resources/application-prod.yml`).

### 학교 서버에 자동 배포

`.github/workflows/deploy.yml`이 main에 푸시하거나 머지할 때마다 화면과 서버를 jar 하나로 빌드하고 `Dockerfile`로 도커 이미지를 만든다. 이미지를 학교 서버의 `~/blog-dock`에 올리고 `deploy/deploy.sh`가 SSH로 접속해 블로그를 롤링 배포로 바꾼다 (다른 학생들 블로그처럼 `-p 포트:8080`으로 연다).

**롤링 배포**: 블로그를 두 개(`seohyun-blog`는 `APP_PORT`=8380, `seohyun-blog-2`는 `APP_PORT_2`=8381, 없으면 APP_PORT + 1) 띄워 두고, 배포할 때 2번을 먼저 새 버전으로 바꿔 켜진 걸(`/api/health`) 확인한 뒤 1번을 바꾼다. 2번이 안 켜지면 멈추고 1번은 옛 버전으로 둔다. `deploy/nginx/seohyun.java21.net.conf`가 두 포트를 번갈아 쓰고, 꺼진 쪽으로 간 요청은 다른 쪽으로 다시 보낸다. 로그인 세션은 DB(`V10__spring_session.sql`, Spring Session JDBC)에 저장해서 어느 블로그로 가도 로그인이 유지되고, 배포해도 로그아웃되지 않는다. 끌 때는 처리 중인 요청을 마저 끝낸다(`server.shutdown: graceful`).
- 내 컴퓨터에서 도커(MySQL 8 + nginx)로 확인: 배포하는 동안 0.05초마다 요청 1,619개를 보내 실패 0개. nginx가 8380만 가리키는 예전 방식은 같은 시험에서 190개(약 15초) 실패
- 잠깐은 옛 버전과 새 버전이 같이 돈다. DB 마이그레이션은 추가만 하고, 컬럼 삭제·이름 바꾸기는 그다음 배포에서 한다
- 블로그가 둘이라 서버 메모리를 최대 1GB쯤 쓴다

1. 저장소 Settings → Secrets and variables → Actions의 **Secrets**에 `DB_ADDRESS`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SSH_ADDRESS`, `SSH_ID`, `SSH_PASSWORD`, `SSH_PORT`를 넣는다.
2. 같은 화면의 **Variables** 탭에 블로그 포트 `APP_PORT`를 넣는다. 이 값이 없으면 배포하지 않는다.
3. 코드를 올리면 Actions 탭에서 진행 상황을 볼 수 있다. 서버에서 로그는 `docker logs seohyun-blog`, 손으로 다시 띄우려면 `~/blog-dock/deploy.sh`.

## 어디에 무엇이 있나

| 위치 | 내용 |
| --- | --- |
| `backend/` | Spring Boot 3.5 (Java 17). `src/main/resources/db/migration/V1__init.sql`이 1차 ERD의 테이블 11개 |
| `frontend/` | React 19 + Vite |
| `deploy/` | 학교 서버 배포 스크립트(롤링 배포)와 nginx 설정 |
| `Dockerfile`, `.github/workflows/deploy.yml` | 도커 이미지와 자동 배포 |

## 문서

기능명세서(spec.md), 설계 계획(plan.md), 작업 목록(tasks.md), ERD, 프로젝트 원칙(constitution.md)과 Spec Kit 명령은 [blog-dock-docs](https://github.com/blog-parkseo/blog-dock-docs) 저장소에 있다.
