---

description: "티스토리형 블로그 서비스 구현 작업 목록"
---

# Tasks: 티스토리형 블로그 서비스

**Input**: Design documents from `/specs/001-tistory-blog/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/rest-api.md](./contracts/rest-api.md), [quickstart.md](./quickstart.md)

**Tests**: 포함한다. plan.md(R15)와 성공 기준 SC-003·SC-004·SC-005가 권한표, 공개 범위, 중복 요청을 자동 테스트로 확인하도록 요구한다. 각 스토리의 테스트를 먼저 쓰고 실패를 확인한 뒤 구현한다.

**Organization**: 사용자 스토리별로 묶었다. 작업 설명 끝의 `(POST-01)` 같은 괄호는 spec.md의 기능 ID다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 앞선 미완료 작업에 기대지 않아 동시에 할 수 있음
- **[Story]**: US1~US10 (spec.md의 User Story 1~10)
- 경로 약어: `BE` = `backend/src/main/java/com/blogdock`, `BT` = `backend/src/test/java/com/blogdock`, `DB` = `backend/src/main/resources/db/migration`, `FE` = `frontend/src`

## Path Conventions

- 웹 애플리케이션 구조: `backend/`(Spring Boot), `frontend/`(React + Vite). plan.md의 Project Structure를 따른다.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 프로젝트 골격과 도구

- [ ] T001 Create `backend/pom.xml` with Spring Boot 3.3.x parent, Java 17, dependencies: spring-boot-starter-web, -data-jpa, -validation, -security, -oauth2-client, -cache, flyway-core, flyway-mysql, h2, mysql-connector-j, commonmark, owasp-java-html-sanitizer, thumbnailator, metadata-extractor, caffeine, spring-boot-starter-test, spring-security-test
- [ ] T002 Create `BE/BlogDockApplication.java` (with `@EnableScheduling`, `@EnableCaching`) and `backend/src/main/resources/application.yml` (H2 file DB `jdbc:h2:file:./data/blogdock;MODE=MySQL`, profiles `local`/`test`/`prod`, env vars `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `APP_DOMAIN`, `ADMIN_SOCIAL_IDS`, `UPLOAD_DIR`, JPA `ddl-auto: validate`, time zone UTC)
- [ ] T003 [P] Create `frontend/` with Vite 5 + React 18 (`frontend/package.json`, `frontend/index.html`, `FE/main.jsx`), add react-router-dom 6, @tanstack/react-query 5, @toast-ui/editor 3, dompurify, vitest, @testing-library/react, @playwright/test
- [ ] T004 [P] Configure `frontend/vite.config.js`: `server.host: true`, `allowedHosts: ['.blogdock.localhost']`, proxy `/api`, `/files`, `/oauth2`, `/login/oauth2` → `http://localhost:8080` with `changeOrigin: false` (Host 헤더 유지)
- [ ] T005 [P] Add `.gitignore` entries (`backend/data/`, `backend/.env.local`, `frontend/node_modules/`, `frontend/dist/`) and `.editorconfig`
- [ ] T006 [P] Add GitHub Actions CI `.github/workflows/ci.yml`: `mvn -B verify` in backend, `npm ci && npm test && npm run build` in frontend

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 스토리가 기대는 공통 기반

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 사용자 스토리 작업을 시작하지 않는다

- [ ] T007 Create Flyway migration `DB/V1__core.sql` with P0 tables from data-model.md: USER (`(social_provider, social_id)` UK, role `MEMBER`/`ADMIN`), BLOG (`address VARCHAR(32) UK NN`, `name VARCHAR(40) NN`, `active_owner_id` generated column + UK, `topic_id` nullable), RESERVED_WORD (seed: www, api, admin, login, logout, files, static, mail, help, blog, oauth2, manage, search, feed, notice), TOPIC (seed: IT, 여행, 반려동물, 일상, 맛집, 취미), CATEGORY (`name VARCHAR(20)`, `parent_key` generated column, `(blog_id, parent_key, name)` UK), POST (`title VARCHAR(100)`, `content_markdown/html/text MEDIUMTEXT NN`, `slug VARCHAR(100)`, `(blog_id, slug)` UK, `visibility`, `status`, `published_at`, `hidden_at`), TAG (`(blog_id, name)` UK), POST_TAG (복합 PK), IMAGE, COMMENT (`content VARCHAR(1000) NN`, `deleted_at`, `hidden_at`), POST_LIKE (복합 PK), IDEMPOTENCY_RECORD (`(user_id, idem_key)` PK), ADMIN_LOG; plus the `[V1]` indexes from `specs/001-tistory-blog/sql/indexes.sql`
- [ ] T008 [P] Create JPA base: `BE/common/BaseTimeEntity.java` (`createdAt`, `updatedAt` as `Instant`), `BE/common/Clock` bean for testable time
- [ ] T009 [P] Create `BE/auth/User.java` entity + `UserRepository.java` (role enum `MEMBER`, `ADMIN`)
- [ ] T010 Implement `BE/config/SecurityConfig.java`: OAuth2 login with Kakao provider (authorization/token/user-info URIs, `client-authentication-method: client_secret_post`), session cookie `SESSION` HttpOnly SameSite=Lax `Domain=${APP_DOMAIN}`, CSRF via `CookieCsrfTokenRepository` (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header), Origin check rejecting other sites, 401 JSON for `/api/**` instead of redirect (COM-01-1, NFR-03, NFR-04)
- [ ] T011 Implement `BE/auth/KakaoOAuth2UserService.java`: upsert USER by `(KAKAO, id)`, nickname from Kakao profile, role `ADMIN` only if id in `ADMIN_SOCIAL_IDS` (ADMIN-01-2); and `BE/auth/LoginSuccessHandler.java` redirecting to `redirect` param only when its host is `APP_DOMAIN` or `*.APP_DOMAIN`, else home (AUTH-01-2); `LoginFailureHandler` → `/login?error=` (AUTH-01-3)
- [ ] T012 [P] Implement `BE/auth/CurrentUser.java` argument resolver and `BE/auth/AuthFacade.java` (`currentUserOrNull()`, `requireUser()`)
- [ ] T013 [P] Implement global error handling `BE/config/ApiExceptionHandler.java` and `BE/common/ApiException.java` hierarchy (`NotFound` 404, `Forbidden` 403, `Conflict` 409, `Validation` 400 with `fields[]`, `PayloadTooLarge` 413, `UnsupportedMedia` 415, `Locked` 423); body `{code, message, fields}`; unknown errors → 500 with generic Korean message, no stack/SQL (COM-02-1~3)
- [ ] T014 [P] Implement `BE/common/text/TextRules.java`: trim-then-length helpers for title 1~100, comment 1~1000, blog name 1~40, category name 1~20, tags max 10 (POL-05)
- [ ] T015 [P] Implement idempotency: `BE/common/idempotency/IdempotencyRecord.java`, `IdempotencyService.java` and `@Idempotent` annotation + `IdempotencyInterceptor.java` (requires `Idempotency-Key` UUID header, stores first status/body per user for 24h, 422 if same key with different request hash) (NFR-08)
- [ ] T016 [P] Implement pagination DTOs `BE/common/page/PageResponse.java` (`items, page, size, totalItems, totalPages`, page beyond last → empty items) and `CursorResponse.java` + `PostCursor.java` (opaque base64 of `publishedAt,id`) (POL-04-2~4)
- [ ] T017 [P] Implement `BE/common/text/MarkdownRenderer.java` (commonmark-java → HTML, then OWASP sanitizer policy allowing h1-h6, p, strong, em, ul, ol, li, blockquote, pre, code, a[href, http/https only, rel=nofollow], img[src from `/files/` only], br, hr; plus plain text extraction for `content_text`) (POST-01-4, NFR-01)
- [ ] T018 [P] Implement image upload in `BE/image/`: `Image.java`, `ImageRepository.java`, `ImageService.java` (magic-byte detection for jpeg/png/gif/webp only, max 10MB → 413, non-image → 415, EXIF orientation fix via metadata-extractor, 400px thumbnail via Thumbnailator, GIF stored as-is), `ImageController.java` `POST /api/images`, static serving `/files/**` from `UPLOAD_DIR` (POL-06, POST-05)
- [ ] T019 Implement `BE/post/PostAccessPolicy.java` with the 8-step visibility order from data-model.md (존재 → 소속 → 블로그 삭제·제한 → 관리자 숨김 → 주인 → PUBLISHED → visibility → 카테고리 비공개), returning 404 on any failure; plus a reusable JPA `Specification`/query fragment `visibleTo(viewer)` used by every list and count (POST-04-2, POL-01-3, POL-04-5)
- [ ] T020 [P] Implement `BE/config/WebConfig.java` (register interceptor/resolvers, Jackson ISO-8601 UTC) and `GET /api/health`
- [ ] T021 [P] Frontend foundation: `FE/api/client.js` (fetch wrapper adding `X-XSRF-TOKEN` from cookie, `Idempotency-Key` helper, 401 → redirect to login with current URL, error object with `fields`), `FE/api/queryClient.js`
- [ ] T022 [P] Frontend foundation: `FE/app/host.js` (parse `window.location.hostname` → `{ isBlog, blogAddress }` for `*.blogdock.localhost`), `FE/app/Router.jsx` (service routes vs blog routes), `FE/lib/time.js` (UTC → Asia/Seoul display), `FE/lib/sanitize.js` (DOMPurify second layer)
- [ ] T023 [P] Frontend foundation: `FE/pages/ErrorPage.jsx` (404/403/500 with links to 홈·이전 화면), `FE/components/EmptyState.jsx`, `FE/components/Pagination.jsx`, base responsive CSS `FE/styles/base.css` (no horizontal scroll at 360px) (COM-02-1, NFR-13)
- [ ] T024 Test infrastructure: `BT/support/IntegrationTest.java` (`@SpringBootTest` + MockMvc + H2 `test` profile), `BT/support/TestUsers.java` (`asGuest()`, `asMember(id)`, `asOwner(blog)`, `asAdmin()` with mock OAuth2 login and CSRF), `frontend/playwright.config.js` with a `test` backend profile that exposes `POST /api/test/login` (only in `test` profile)
- [ ] T025 Verify shared-cookie decision (research.md R5): manual check that a session created on `blogdock.localhost:5173` is sent to `my-dog.blogdock.localhost:5173` in Chrome, Safari, Edge; if not, switch `APP_DOMAIN` default to `lvh.me` and note it in `specs/001-tistory-blog/research.md`

**Checkpoint**: `/api/health`가 서브도메인에서 열리고 로그인 쿠키가 공유된다. 사용자 스토리 작업을 시작할 수 있다.

---

## Phase 3: User Story 1 - 가입하고 내 블로그를 연다 (Priority: P1 · 명세 P0) 🎯 MVP

**Goal**: 카카오 로그인 → 블로그 개설 → 정보 수정 → 로그아웃

**Independent Test**: 새 계정으로 로그인 → 블로그 개설 → 블로그 메인 접속 → 정보 수정 → 로그아웃

### Tests for User Story 1 ⚠️

- [ ] T026 [P] [US1] Auth API test in `BT/auth/AuthApiTest.java`: `GET /api/me` 401 for guest, 200 with `blogAddress` null for new member; `POST /api/auth/logout` → session invalid; new Kakao user always `MEMBER` (AUTH-01, AUTH-02, ADMIN-01-2)
- [ ] T027 [P] [US1] Blog address test in `BT/blog/BlogAddressTest.java`: `ab`, 33자, `ABC`, `-abc`, `abc-`, `admin`, taken address → each rejected with reason `FORMAT`/`RESERVED`/`TAKEN`; `abcd` and 32자 accepted; deleted blog's address stays taken (POL-02-2~4)
- [ ] T028 [P] [US1] Blog API test in `BT/blog/BlogApiTest.java`: create → 201; second blog → 409; PATCH name/description/profile works and `address` field is ignored; non-owner PATCH → 403; unknown address → 404 (BLOG-01, BLOG-02)

### Implementation for User Story 1

- [ ] T029 [P] [US1] Create `BE/blog/Blog.java`, `BlogRepository.java`, `ReservedWordRepository.java`, `BE/blog/Topic.java` entities per data-model.md (`address` 4~32자 `^[a-z0-9](?:[a-z0-9-]{2,30})[a-z0-9]$`, `name` 1~40자)
- [ ] T030 [US1] Implement `BE/blog/BlogAddressValidator.java` (format → reserved → taken, returns reason) and `BlogService.java` (create with one-active-blog rule, update name/description/profileImage, `requireOwner(address, user)`) (BLOG-01, BLOG-02, POL-02)
- [ ] T031 [US1] Implement `BE/blog/BlogController.java`: `GET /api/blogs/address-check`, `POST /api/blogs`, `GET /api/blogs/{address}`, `PATCH /api/blogs/{address}`; `BE/auth/MeController.java`: `GET /api/me`, `POST /api/auth/logout`
- [ ] T032 [P] [US1] Frontend `FE/pages/LoginPage.jsx` (카카오 로그인 버튼 → `/oauth2/authorization/kakao?redirect=`, shows `error` reason) and `FE/components/Header.jsx` (로그인/로그아웃, 내 블로그, 글쓰기) (AUTH-01, AUTH-02)
- [ ] T033 [P] [US1] Frontend `FE/pages/CreateBlogPage.jsx`: address input with live `address-check` and reason messages, warning "주소는 나중에 바꿀 수 없어요", name, optional description; on success navigate to `http://{address}.{APP_DOMAIN}` (BLOG-01)
- [ ] T034 [P] [US1] Frontend `FE/pages/manage/BlogSettingsPage.jsx`: edit name, description, profile image (upload via `/api/images`), address shown read-only (BLOG-02)

**Checkpoint**: Story 1을 혼자 시연할 수 있다.

---

## Phase 4: User Story 2 - 글을 쓰고 발행하고 고친다 (Priority: P2 · 명세 P0)

**Goal**: 글 발행·수정·공개 범위 변경·삭제, 이미지·카테고리·태그 포함

**Independent Test**: 이미지가 든 글 발행 → 상세 확인 → 수정 → 비공개 전환 → 삭제

### Tests for User Story 2 ⚠️

- [ ] T035 [P] [US2] Publish test in `BT/post/PostPublishTest.java`: empty title+body → only title error; whitespace title → empty; title 101자 → 400; no category → 미분류; 10 requests with same `Idempotency-Key` → 1 post; counts start at 0; returned slug from title, duplicate title → `-2`; 한글 title slug opens via URL-encoded path (POST-01, POL-03, NFR-08)
- [ ] T036 [P] [US2] Edit/delete test in `BT/post/PostEditDeleteTest.java`: edit keeps slug, publishedAt, order, counts; non-owner edit/delete → 403, guest → 401; delete removes comments/likes/tags in one transaction and old URL → 404 (POST-02, POST-03, POL-07-1, NFR-11)
- [ ] T037 [P] [US2] Sanitizer test in `BT/common/MarkdownRendererTest.java`: `<script>`, `onerror=`, `javascript:` links removed; headings, bold, lists, quote, code block, link, `/files/` image kept (POST-01-4, NFR-01)
- [ ] T038 [P] [US2] Image test in `BT/image/ImageServiceTest.java`: png renamed `.jpg` accepted by magic bytes, text file renamed `.png` → 415, 11MB → 413, EXIF-rotated jpeg stored upright, GIF bytes unchanged (POL-06)
- [ ] T039 [P] [US2] Category/tag test in `BT/category/CategoryTagTest.java`: duplicate name same level → 409; delete moves posts to 미분류; 11 tags → 400; same tag twice → one; new tag name creates tag (CAT-01, TAG-01)

### Implementation for User Story 2

- [ ] T040 [P] [US2] Create `BE/post/Post.java` (status `DRAFT`/`SCHEDULED`/`PUBLISHED`, visibility `PUBLIC`/`PRIVATE`/`SUBSCRIBER`, `title VARCHAR(100)`, slug immutable after first publish), `PostRepository.java`
- [ ] T041 [P] [US2] Create `BE/category/Category.java` (`name VARCHAR(20)`, `parent_id` one level), `CategoryRepository.java`; `BE/tag/Tag.java`, `PostTag.java`, `TagRepository.java`
- [ ] T042 [US2] Implement `BE/post/SlugGenerator.java` (trim → spaces to `-` → keep 한글/영문/숫자/`-` → max 80 → `-2`, `-3` on collision in blog → fallback `post-{id}`) (POL-03-1·2)
- [ ] T043 [US2] Implement `BE/tag/TagService.java` (normalize, dedupe case-insensitively, max 10, create missing per blog) (TAG-01)
- [ ] T044 [US2] Implement `BE/category/CategoryService.java` + `CategoryController.java`: create/rename/delete with same-level uniqueness, delete moves posts to null category in one transaction (CAT-01, POL-07-3)
- [ ] T045 [US2] Implement `BE/post/PostService.java`: publish (validate title → body order, render markdown, slug, `publishedAt = now`), update (keep slug/publishedAt), change visibility, delete with cascade in one `@Transactional` (POST-01~03, POST-06)
- [ ] T046 [US2] Implement `BE/post/PostController.java`: `POST /api/blogs/{address}/posts` (`@Idempotent`), `PUT /api/blogs/{address}/posts/{id}`, `PATCH …/{id}/visibility`, `DELETE …/{id}`; owner-only via `BlogService.requireOwner`
- [ ] T047 [P] [US2] Frontend `FE/components/editor/PostEditor.jsx`: Toast UI Editor with markdown + WYSIWYG tabs, image upload hook → `/api/images` inserting at cursor in selection order, loads `contentMarkdown` for edit (POST-01-4, POST-05)
- [ ] T048 [US2] Frontend `FE/pages/WritePage.jsx`: title, editor, category select (none = 미분류), tag input (max 10), visibility radio (default 공개), publish button disabled while pending + `Idempotency-Key`, field errors shown, content kept on failure (POST-01, POST-06, NFR-10)
- [ ] T049 [P] [US2] Frontend `FE/pages/manage/CategoryManagePage.jsx`: add/rename/delete with confirm dialog (CAT-01)
- [ ] T050 [US2] Frontend edit/delete flow in `FE/pages/WritePage.jsx` (edit mode) and `FE/components/post/OwnerActions.jsx` (수정·삭제, delete confirm → 블로그 메인) (POST-02, POST-03)

**Checkpoint**: Story 2를 혼자 시연할 수 있다.

---

## Phase 5: User Story 3 - 다른 사람이 홈에서 글을 발견하고 읽는다 (Priority: P3 · 명세 P0)

**Goal**: 홈 최신 글, 글 상세, 블로그 메인·사이드바, 카테고리·태그 목록, 블로그 내 검색

**Independent Test**: 다른 블로그에 공개 15개·비공개 1개를 두고 비회원으로 홈 → 상세 → 메인 → 카테고리 → 태그 → 검색

### Tests for User Story 3 ⚠️

- [ ] T051 [P] [US3] Visibility test in `BT/post/VisibilityTest.java`: other's PRIVATE post absent from home, blog list, category, tag, search, sidebar counts, and direct URL → 404; owner sees it everywhere; wrong blog address + valid slug → 404 (POL-01, POST-04-2, SC-004)
- [ ] T052 [P] [US3] List test in `BT/post/PostListTest.java`: order `published_at DESC, id DESC`; page size 10; page beyond last → empty 200; home cursor paging with a post inserted between calls → no duplicate/missing (POL-04, HOME-01)
- [ ] T053 [P] [US3] Search test in `BT/search/BlogSearchTest.java`: `"  Dog  "` matches `dog` in title/body/tag case-insensitively; whitespace-only → 400 `EMPTY_QUERY`; results exclude invisible posts (SRCH-01)
- [ ] T054 [P] [US3] Sidebar test in `BT/blog/SidebarTest.java`: 전체 글 count, categories with counts, 미분류 count, viewer-based counts (BLOG-04)

### Implementation for User Story 3

- [ ] T055 [US3] Implement `BE/post/PostQueryService.java`: blog list (page, `categoryId` incl. `uncategorized`, `tag`), detail by `(address, slug)` through `PostAccessPolicy`, summaries with `excerpt` (평문 150자), `likeCount`, `commentCount` computed by COUNT (BLOG-03, POST-04, CAT-02, TAG-02)
- [ ] T056 [US3] Implement `BE/post/PostQueryController.java`: `GET /api/blogs/{address}/posts`, `GET /api/blogs/{address}/posts/{slug}`
- [ ] T057 [US3] Implement `BE/blog/SidebarService.java` + `GET /api/blogs/{address}/sidebar` (BLOG-04)
- [ ] T058 [P] [US3] Implement `BE/home/HomeService.java` + `HomeController.java`: `GET /api/home/latest?cursor=` public, published, not hidden, blog not restricted/deleted (HOME-01)
- [ ] T059 [P] [US3] Implement `BE/search/SearchService.java` + `GET /api/blogs/{address}/search?q=&page=` using `LOWER(...) LIKE` on title, content_text, tag name (SRCH-01)
- [ ] T060 [P] [US3] Frontend `FE/pages/HomePage.jsx`: latest posts with 제목·블로그 이름·발행일·요약, "더보기" cursor loading, empty state (HOME-01)
- [ ] T061 [P] [US3] Frontend `FE/components/blog/BlogLayout.jsx` + `Sidebar.jsx` (블로그 이름·프로필 → 메인, 전체 글 맨 위, 미분류 맨 아래·0이면 숨김, 소개 비면 숨김) (BLOG-04, BLOG-02-2)
- [ ] T062 [P] [US3] Frontend `FE/pages/blog/BlogMainPage.jsx`, `CategoryPostsPage.jsx`, `TagPostsPage.jsx` (page-based, unique URLs `/category/{id}`, `/tag/{name}`) (BLOG-03, CAT-02, TAG-02)
- [ ] T063 [US3] Frontend `FE/pages/blog/PostDetailPage.jsx` at `/entry/{slug}`: title, date (KST), category, sanitized `contentHtml`, tags, like count, comments slot, owner actions (POST-04)
- [ ] T064 [P] [US3] Frontend `FE/pages/blog/BlogSearchPage.jsx` at `/search?q=`: trims query, blocks whitespace-only, keeps query on no results (SRCH-01)

**Checkpoint**: Story 3을 혼자 시연할 수 있다.

---

## Phase 6: User Story 4 - 읽은 글에 댓글과 공감을 남긴다 (Priority: P4 · 명세 P0)

**Goal**: 회원 댓글·공감, 블로그 주인의 댓글 삭제

**Independent Test**: A의 글에 B가 댓글·공감 → A가 B 댓글 삭제 → 비회원 공감 시도

### Tests for User Story 4 ⚠️

- [ ] T065 [P] [US4] Comment test in `BT/comment/CommentApiTest.java`: guest → 401; 1~1000자 rule; order `created_at ASC`; same `Idempotency-Key` ×10 → 1 comment; author delete ok; other member delete → 403; blog owner delete other's comment ok, owner edit other's → 403 (CMT-01, CMT-02)
- [ ] T066 [P] [US4] Like test in `BT/reaction/LikeApiTest.java`: PUT ×10 → liked once, count 1; DELETE → 0; guest → 401; own post → 403 (SOC-01)

### Implementation for User Story 4

- [ ] T067 [P] [US4] Create `BE/comment/Comment.java` (`content VARCHAR(1000) NN`, plain text), `CommentRepository.java`; `BE/reaction/PostLike.java` (복합 PK), `PostLikeRepository.java`
- [ ] T068 [US4] Implement `BE/comment/CommentService.java` + `CommentController.java`: `GET /api/posts/{postId}/comments` (only if post visible), `POST` (`@Idempotent`), `DELETE /api/comments/{id}` (author or blog owner) (CMT-01, CMT-02)
- [ ] T069 [US4] Implement `BE/reaction/LikeService.java` + `LikeController.java`: `PUT`/`DELETE /api/posts/{postId}/like` → `{liked, likeCount}`, reject own post (SOC-01, SOC-01-3)
- [ ] T070 [P] [US4] Frontend `FE/components/comment/CommentSection.jsx`: list with 닉네임·KST 날짜와 시각·count, form (guest → login with return URL), delete with confirm for own/owner (CMT-01, CMT-02)
- [ ] T071 [P] [US4] Frontend `FE/components/post/LikeButton.jsx`: optimistic toggle via TanStack Query, rollback on error, guest → login (SOC-01, NFR-07)

**Checkpoint**: P0 흐름 "가입 → 개설 → 발행 → 발견 → 읽기·댓글·공감"이 한 바퀴 돈다.

---

## Phase 7: User Story 5 - 접근 제어와 서비스 관리자 영역 (Priority: P5 · 명세 P0)

**Goal**: 권한표 전체를 서버에서 보장, 관리 영역 보호, 오류 처리 마무리

**Independent Test**: 네 가지 사용자 상태로 권한표의 모든 행동을 화면·주소·직접 요청으로 시도

### Tests for User Story 5 ⚠️

- [ ] T072 [P] [US5] Permission matrix test in `BT/security/PermissionMatrixTest.java`: parameterized over guest/member/owner/admin × every row of the COM-01 table in spec.md, asserting allow / 401 / 403 / 404 exactly (COM-01, SC-003)
- [ ] T073 [P] [US5] Admin area test in `BT/admin/AdminAreaTest.java`: `GET /api/admin/ping` guest 401, member 403, admin 204; at least one admin exists after startup with `ADMIN_SOCIAL_IDS` (ADMIN-01)
- [ ] T074 [P] [US5] Security test in `BT/security/CsrfOriginTest.java`: state-changing request without `X-XSRF-TOKEN` → 403; foreign `Origin` → 403; 500 body has no stack trace (NFR-03, COM-02-3)

### Implementation for User Story 5

- [ ] T075 [US5] Implement `BE/admin/AdminController.java` `GET /api/admin/ping` with `hasRole('ADMIN')` on `/api/admin/**` in `SecurityConfig.java`; startup check `BE/admin/AdminBootstrap.java` warns if no admin configured (ADMIN-01)
- [ ] T076 [US5] Fix any gaps found by T072 in service-layer owner/author checks (BlogService, PostService, CommentService, CategoryService)
- [ ] T077 [P] [US5] Frontend `FE/pages/admin/AdminHomePage.jsx` route guard (403 page for member, login for guest) (ADMIN-01)
- [ ] T078 [P] [US5] Frontend draft protection `FE/lib/draftBackup.js`: save title/body/comment text to `sessionStorage` before 401 redirect or failed save, restore after login (COM-01-1, NFR-10)

**Checkpoint**: P0 24개 완료. quickstart.md 수동 시나리오 1~14와 E2E가 통과해야 한다.

---

## Phase 8: User Story 6 - 쓸 만한 블로그로 다듬기 (Priority: P6 · 명세 P1)

**Goal**: 임시저장, 대표 이미지, 조회수, 이전·다음 글, 하위 카테고리·순서, 태그 목록, 댓글 수정, 방명록, 공유, 로그인 유지, 개설 안내, 회원정보 수정

**Independent Test**: 임시저장 후 이어 쓰기 → 대표 이미지 → 하위 카테고리 글 확인 → 방명록 → 주소 복사

- [ ] T079 [US6] Create migration `DB/V2__p1.sql`: GUESTBOOK (`content VARCHAR(1000) NN`), POST_VIEW_LOG (`viewer_key CHAR(64)`, index `(post_id, viewer_key, viewed_at)`), SUBSCRIPTION (복합 PK), USER_SANCTION, `CATEGORY.sort_order`, `POST.thumbnail_url`; plus the `[V2]` indexes from `specs/001-tistory-blog/sql/indexes.sql`
- [ ] T080 [P] [US6] Test `BT/post/DraftTest.java`: draft visible only to owner, publishing a draft turns that row PUBLISHED and removes it from drafts (POST-08)
- [ ] T081 [P] [US6] Test `BT/post/ViewCountTest.java`: same viewer within 30분 counted once, after 30분 counted again (POST-09)
- [ ] T082 [P] [US6] Test `BT/category/SubCategoryTest.java`: depth 2 → 400, parent with children delete → 409, parent list includes children posts and summed count, same name under different parents ok (CAT-03)
- [ ] T083 [US6] Implement drafts in `BE/post/PostService.java` + `GET /api/blogs/{address}/drafts` (`status=DRAFT`, `draftId` on publish) (POST-08)
- [ ] T084 [P] [US6] Implement thumbnail selection in `BE/post/PostService.java` (explicit `thumbnailImageId` else first `/files/` image in content else null) (POST-07)
- [ ] T085 [P] [US6] Implement `BE/post/ViewCountService.java` (viewer key = SHA-256 of user id or `vid` cookie, 30분 window) called from detail endpoint (POST-09)
- [ ] T086 [P] [US6] Implement prev/next in `BE/post/PostQueryService.java` using `visibleTo(viewer)` ordering by `published_at, id` (POST-10)
- [ ] T087 [US6] Extend `BE/category/CategoryService.java`: one-level children, `PUT /api/blogs/{address}/categories/order` (CAT-03, CAT-04)
- [ ] T088 [P] [US6] Implement `GET /api/blogs/{address}/tags` in `BE/tag/TagController.java` with visible post counts (TAG-03)
- [ ] T089 [P] [US6] Implement `PATCH /api/comments/{id}` author-only in `BE/comment/CommentController.java` (CMT-03)
- [ ] T090 [P] [US6] Implement guestbook `BE/comment/Guestbook.java`, `GuestbookService.java`, `GuestbookController.java` (same rules as comments, owner can delete all) (CMT-04)
- [ ] T091 [P] [US6] Implement `PATCH /api/me` nickname/profile image in `BE/auth/MeController.java`, session timeout 14 days sliding in `application.yml` (AUTH-03, AUTH-05)
- [ ] T092 [P] [US6] Frontend: draft resume prompt in `FE/pages/WritePage.jsx`, thumbnail picker `FE/components/editor/ThumbnailPicker.jsx` (POST-07, POST-08)
- [ ] T093 [P] [US6] Frontend: prev/next links and view count in `FE/pages/blog/PostDetailPage.jsx`; copy-link button `FE/components/post/ShareButton.jsx` (POST-09, POST-10, SOC-02)
- [ ] T094 [P] [US6] Frontend: sub-category and drag order in `FE/pages/manage/CategoryManagePage.jsx`, nested list in `FE/components/blog/Sidebar.jsx`, tag cloud `FE/components/blog/TagList.jsx` (CAT-03, CAT-04, TAG-03)
- [ ] T095 [P] [US6] Frontend: `FE/pages/blog/GuestbookPage.jsx`, comment edit in `FE/components/comment/CommentSection.jsx`, `FE/pages/AccountPage.jsx` (CMT-03, CMT-04, AUTH-05)
- [ ] T096 [P] [US6] Frontend: write button for user without blog → `CreateBlogPage` in `FE/components/Header.jsx` (AUTH-04)

---

## Phase 9: User Story 7 - 구독하고 피드로 모아 본다 (Priority: P7 · 명세 P1)

**Goal**: 블로그 구독·해제, 피드, 구독자 수

**Independent Test**: A가 B 구독 → B 발행 → A 피드 표시 → 해제

- [ ] T097 [P] [US7] Test `BT/subscription/SubscriptionTest.java`: own blog → 400; PUT ×10 → one row; count matches after refresh; feed cursor no duplicate/missing; empty feed → empty items (SUB-01~03)
- [ ] T098 [US7] Implement `BE/subscription/Subscription.java`, `SubscriptionService.java`, `SubscriptionController.java`: `PUT/DELETE /api/blogs/{address}/subscription`, `GET /api/me/subscriptions` (SUB-01, SUB-03)
- [ ] T099 [US7] Implement `BE/subscription/FeedService.java` + `GET /api/feed?cursor=` (subscribed blogs, `visibleTo(viewer)`) (SUB-02)
- [ ] T100 [P] [US7] Frontend `FE/components/blog/SubscribeButton.jsx` (optimistic, shows subscriber count) and `FE/pages/FeedPage.jsx` (empty → 둘러보기 링크), `FE/pages/SubscriptionsPage.jsx` (SUB-01~03)

---

## Phase 10: User Story 8 - 인기 글·주제별 글·통합 검색 (Priority: P8 · 명세 P1)

**Goal**: 홈 인기 글, 블로그 주제와 주제별 글, 통합 검색

**Independent Test**: 7일 공감·댓글이 다른 글 3개로 순서 확인 → 블로그 주제 지정 → 주제별 글 → 통합 검색

- [ ] T101 [P] [US8] Test `BT/home/PopularTest.java`: rank by likes+comments in last 7 days, excludes hidden/private, empty → fallback flag (HOME-02)
- [ ] T102 [P] [US8] Test `BT/search/GlobalSearchTest.java`: finds public posts and non-restricted blogs by name/description, post result includes blog name (SRCH-02)
- [ ] T103 [US8] Implement `BE/home/PopularService.java` with `@Cacheable` (Caffeine, 10분) + `GET /api/home/popular` (HOME-02)
- [ ] T104 [US8] Implement topic: `PATCH /api/blogs/{address}` accepts `topicId`, `GET /api/topics`, `GET /api/topics/{id}/posts?cursor=` in `BE/home/TopicController.java` (POST-11, HOME-03)
- [ ] T105 [P] [US8] Implement `GET /api/search?q=&type=` in `BE/search/SearchService.java` (SRCH-02)
- [ ] T106 [P] [US8] Frontend: popular section and topic tabs in `FE/pages/HomePage.jsx`, `FE/pages/TopicPage.jsx` at `/topic/{id}`, topic select in `BlogSettingsPage.jsx`, `FE/pages/SearchPage.jsx` (HOME-02, HOME-03, POST-11, SRCH-02)

---

## Phase 11: User Story 9 - 블로그 관리와 서비스 관리자 제재 (Priority: P9 · 명세 P1)

**Goal**: 내 글·댓글 관리, 회원 이용 제한, 글·댓글 숨김

**Independent Test**: 관리자가 회원 3일 제한·글 숨김 → 로그인 차단·사유 확인 → 기한 후 자동 해제 → 글 관리에서 숨김 사유 확인

- [ ] T107 [P] [US9] Test `BT/admin/SanctionTest.java`: sanctioned user → 423 with reason/endsAt on login and next request; auto-lifted after `ends_at` (Clock 조작); admin cannot sanction admin → 403; every action writes ADMIN_LOG (ADMIN-02)
- [ ] T108 [P] [US9] Test `BT/admin/HideTest.java`: hidden post → 404 for others, visible with reason to author, removed from lists/counts, unhide restores slug/order/counts; hidden comment shown as `hiddenByAdmin` with replies kept (ADMIN-03)
- [ ] T109 [US9] Implement `BE/admin/UserSanction.java`, `SanctionService.java`, `SanctionFilter.java` (checks active sanction per request → 423), endpoints `POST /api/admin/users/{id}/sanctions`, `POST /api/admin/sanctions/{id}/release` (ADMIN-02)
- [ ] T110 [US9] Implement `BE/admin/HideService.java` + endpoints `POST /api/admin/posts/{id}/hide|unhide`, `POST /api/admin/comments/{id}/hide|unhide` writing ADMIN_LOG (ADMIN-03)
- [ ] T111 [P] [US9] Implement `BE/manage/ManageController.java`: `GET /api/manage/{address}/posts?status=&visibility=` (incl. drafts, hidden reason), `GET /api/manage/{address}/comments` (comments + guestbook, with post title) (MNG-01, MNG-02)
- [ ] T112 [P] [US9] Frontend `FE/pages/manage/PostManagePage.jsx`, `CommentManagePage.jsx` (MNG-01, MNG-02)
- [ ] T113 [P] [US9] Frontend `FE/pages/admin/UserSanctionPage.jsx`, `ContentHidePage.jsx`, and sanctioned-login notice in `FE/pages/LoginPage.jsx` (ADMIN-02, ADMIN-03)

**Checkpoint**: P1 24개 완료.

---

## Phase 12: User Story 10 - 확장 기능 (Priority: P10 · 명세 P2)

**Goal**: P2 기능을 하나씩 독립적으로 추가 (BLOG-06 블로그 이사·BLOG-08 대표 블로그는 회원당 블로그 1개라 구현하지 않음)

**Independent Test**: 기능마다 spec.md P2 표의 규칙을 테스트로 확인

- [ ] T114 [US10] Create migration `DB/V3__p2.sql`: POST_SAVE, NOTIFICATION, REPORT, NOTICE, VISIT_STAT, BLOG_BLOCKED_USER, BLOG_BANNED_WORD, `COMMENT.parent_id/is_secret`, `POST.comment_allowed/scheduled_at`, `CATEGORY.is_private`, `BLOG.skin/background_image_url/restricted_at/restricted_reason/deleted_at`, `USER.withdrawn_at`; plus the `[V3]` indexes from `specs/001-tistory-blog/sql/indexes.sql`
- [ ] T115 [P] [US10] AUTH-06 회원 탈퇴: `DELETE /api/me` (confirm), clear personal data, show '탈퇴한 회원' — `BE/auth/WithdrawService.java`, test `BT/auth/WithdrawTest.java`
- [ ] T116 [P] [US10] BLOG-05 꾸미기: skins list + background upload/remove — `BE/blog/BlogService.java`, `FE/pages/manage/SkinPage.jsx`
- [ ] T117 [P] [US10] BLOG-07 블로그 삭제: soft delete, address stays reserved, re-create with new address allowed — `BE/blog/BlogService.java`, test `BT/blog/BlogDeleteTest.java`
- [ ] T118 [P] [US10] POST-12 구독자 공개: `SUBSCRIBER` visibility in `PostAccessPolicy` and `visibleTo`, test `BT/post/SubscriberVisibilityTest.java` (excluded from search/home/popular/feed/list for non-subscribers)
- [ ] T119 [P] [US10] POST-13 예약 발행: `BE/post/ScheduledPublisher.java` (`@Scheduled` every minute, `publishedAt` = actual time), test `BT/post/ScheduledPublishTest.java`
- [ ] T120 [P] [US10] CAT-05 카테고리 비공개 in `PostAccessPolicy` step ⑧ and sidebar; TAG-04 태그 이름 변경·삭제 in `BE/tag/TagController.java`
- [ ] T121 [P] [US10] CMT-05 답글(한 단계, 답글 있는 댓글 삭제 → '삭제된 댓글'), CMT-06 비밀댓글, CMT-07 댓글 허용 설정 — `BE/comment/CommentService.java`, test `BT/comment/CommentP2Test.java`
- [ ] T122 [P] [US10] SOC-03 저장: `PUT/DELETE /api/posts/{postId}/save`, `GET /api/me/saved` — `BE/reaction/SaveService.java`, `FE/pages/SavedPage.jsx`
- [ ] T123 [P] [US10] SUB-04 알림 (no self-notification, unread count, read on click) — `BE/subscription/NotificationService.java`, `FE/components/NotificationBell.jsx`
- [ ] T124 [P] [US10] SUB-05 맞구독 (four button states, unsubscribe confirm, no notification) and SUB-06 추천 블로그 (같은 주제, 구독자 수 순, 자기 블로그 제외) — `BE/subscription/`, `FE/components/blog/SubscribeButton.jsx`
- [ ] T125 [P] [US10] HOME-04 인기 블로거, HOME-05 랭킹 전체보기 (same basis and snapshot as home) — `BE/home/PopularService.java`, `FE/pages/RankingPage.jsx`
- [ ] T126 [P] [US10] MNG-03 방문자 통계 (하루 한 번 집계, sidebar same value) and MNG-04 차단·금칙어 — `BE/manage/`, `FE/pages/manage/StatsPage.jsx`, `SpamPage.jsx`
- [ ] T127 [P] [US10] ADMIN-04 신고 (one per target, not own, no auto-hide), ADMIN-05 블로그 이용 제한, ADMIN-06 공지·이력(읽기 전용)·대시보드 — `BE/admin/`, `FE/pages/admin/`

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: 여러 스토리에 걸친 마무리

- [ ] T128 [P] E2E `frontend/tests/e2e/p0-loop.spec.js`: quickstart.md 수동 시나리오 1~13 자동화 (SC-002)
- [ ] T129 [P] Responsive check at 360px for all P0 pages in `frontend/tests/e2e/mobile.spec.js` (NFR-13, SC-008)
- [ ] T130 [P] Performance check: seed 1만 글 (`backend/src/test/resources/seed/`) and verify list/detail p95 ≤ 300ms; add missing indexes in a new migration (SC-006)
- [ ] T131 Security review of upload, sanitizer, redirect param, CSRF, session cookie flags (NFR-01~05)
- [ ] T132 [P] Update `README.md` with run instructions from quickstart.md
- [ ] T133 Run quickstart.md validation end to end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** → **Foundational (Phase 2)** → 모든 사용자 스토리
- **US1 → US2 → US3 → US4 → US5**: P0. US2는 블로그(US1)가 있어야 시연할 수 있고, US3·US4는 글(US2)이 있어야 의미가 있다. 다만 각 스토리의 API·테스트는 Phase 2 이후 데이터 시드로 따로 개발할 수 있다.
- **US5**는 US1~US4의 권한을 모두 검사하므로 마지막 P0 단계다.
- **US6~US9 (P1)**: P0 완료 후. 서로 독립이라 순서를 바꿔도 된다. 단 T079(V2 마이그레이션)를 먼저 한다.
- **US10 (P2)**: P1 완료 후. T114(V3 마이그레이션) 뒤에는 T115~T127이 서로 독립이다.
- **Polish**: 원하는 스토리가 끝난 뒤.

### Within Each User Story

- 테스트 작성 → 실패 확인 → 엔티티 → 서비스 → 컨트롤러 → 화면

### Parallel Opportunities

- Phase 1: T003~T006 동시 진행
- Phase 2: T008, T009, T012~T018, T020~T023 동시 진행 (T010·T011은 T009 뒤, T019는 엔티티 뒤)
- 각 스토리의 테스트 작업([P])은 동시에 쓸 수 있다
- 백엔드와 프론트엔드 작업은 contracts/rest-api.md를 기준으로 동시에 진행할 수 있다

---

## Parallel Example: User Story 2

```bash
# 테스트를 함께 쓴다
Task: "Publish test in backend/src/test/java/com/blogdock/post/PostPublishTest.java"
Task: "Sanitizer test in backend/src/test/java/com/blogdock/common/MarkdownRendererTest.java"
Task: "Image test in backend/src/test/java/com/blogdock/image/ImageServiceTest.java"

# 엔티티를 함께 만든다
Task: "Create Post.java in backend/src/main/java/com/blogdock/post/"
Task: "Create Category.java, Tag.java in backend/src/main/java/com/blogdock/category/, tag/"

# 화면은 API와 동시에
Task: "PostEditor.jsx in frontend/src/components/editor/"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 Setup → Phase 2 Foundational (T025 쿠키 확인 포함)
2. Phase 3 (US1) → 멈추고 확인: 로그인과 블로그 개설
3. US2 → US3 → US4 → US5 순서로 P0 완료 → quickstart.md 시나리오로 시연

### Incremental Delivery

1. P0 완료 = 티스토리형 블로그의 최소 형태 (SC-002)
2. P1 스토리(US6~US9)를 하나씩 추가하고 각각 시연
3. P2 기능은 원하는 것부터 하나씩

### 작업 수

| 구분 | 작업 수 |
| --- | --- |
| Setup | 6 |
| Foundational | 19 |
| US1~US5 (P0) | 53 |
| US6~US9 (P1) | 35 |
| US10 (P2) | 14 |
| Polish | 6 |
| **합계** | **133** |

---

## Notes

- [P] = 다른 파일, 앞선 미완료 작업에 기대지 않음
- 커밋·PR 메시지에 작업 ID와 기능 ID를 함께 적는다 (예: `T045 POST-01 글 발행 서비스`)
- 각 Checkpoint에서 멈추고 그 스토리를 따로 확인한다
