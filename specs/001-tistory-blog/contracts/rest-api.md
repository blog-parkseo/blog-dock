# REST API Contract: 티스토리형 블로그 서비스

**Feature**: [spec.md](../spec.md) | **Data model**: [data-model.md](../data-model.md) | **Date**: 2026-10-07

프론트엔드(React)와 백엔드(Spring Boot) 사이의 계약이다. 모든 경로는 같은 출처의 `/api` 아래에 있다. 블로그는 경로에 `{address}`로 명시한다(서브도메인 해석은 프론트가 하고, API는 주소를 명시적으로 받는다).

## 공통 규칙

| 항목 | 규칙 | 관련 |
| --- | --- | --- |
| 형식 | 요청·응답 JSON(UTF-8). 업로드만 `multipart/form-data` | |
| 인증 | 세션 쿠키(`SESSION`, HttpOnly, SameSite=Lax, Domain=서비스 도메인) | AUTH-01, NFR-04 |
| CSRF | 상태를 바꾸는 요청(POST/PUT/PATCH/DELETE)은 `X-XSRF-TOKEN` 헤더 필수(쿠키 `XSRF-TOKEN` 값). 다른 출처 Origin은 거부 | NFR-03 |
| 중복 방지 | 글 발행·임시저장 생성·댓글·방명록 등록은 `Idempotency-Key: <UUID>` 헤더 필수. 같은 키 재요청은 처음 응답을 그대로 돌려줌 | NFR-08 |
| 시각 | ISO-8601 UTC(`2026-10-07T05:00:00Z`). 화면에서 Asia/Seoul로 변환 | NFR-15 |
| 페이지 목록 | `?page=1&size=10` → `{ "items": [...], "page": 1, "size": 10, "totalItems": 15, "totalPages": 2 }`. 마지막을 넘으면 `items: []`(200) | POL-04-2·4 |
| 커서 목록 | `?cursor=<opaque>&size=20` → `{ "items": [...], "nextCursor": "..." \| null }` | POL-04-3 |
| 오류 | `{ "code": "VALIDATION_FAILED", "message": "사용자용 문구", "fields": [{ "field": "title", "reason": "제목을 입력해 주세요" }] }`. 내부 정보(스택, SQL)는 넣지 않음 | COM-02 |

**상태 코드**: 200/201/204 성공, 400 입력 오류, 401 로그인 필요(프론트가 로그인으로 안내하고 원래 주소를 기억), 403 권한 없음, 404 없음(남의 비공개 글 포함), 409 상태 충돌(중복 주소, 하위 카테고리 존재 등), 413 파일 너무 큼, 415 이미지 아님, 422 같은 Idempotency-Key에 다른 내용, 423 이용 제한된 회원(`{ reason, endsAt }`), 500 알 수 없는 오류(일반 문구만).

## 공통 객체

```text
PostSummary  { id, blog: { address, name }, slug, title, excerpt(평문 앞 150자), thumbnailUrl|null,
               category: { id, name }|null, publishedAt, likeCount, commentCount, visibility }
PostDetail   PostSummary + { contentHtml, tags: [name], liked: bool, isOwner: bool,
               hidden: { reason }|null (주인에게만), prev: {slug,title}|null, next: {slug,title}|null }
BlogInfo     { address, name, description|null, profileImageUrl, topic: {id,name}|null,
               subscriberCount, subscribed: bool, isOwner: bool }
Comment      { id, author: { nickname, profileImageUrl, blogAddress|null }, content, createdAt,
               isMine: bool, deleted: bool, hiddenByAdmin: bool, replies: [Comment] }
```

## AUTH 회원·인증

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `GET /oauth2/authorization/kakao?redirect=<원래 주소>` | 카카오 로그인 시작(브라우저 이동). 성공하면 `redirect`(서비스 도메인 하위만 허용, 없으면 홈)로 돌아감. 실패·취소는 `/login?error=<사유>` | 모두 | AUTH-01 |
| `POST /api/auth/logout` | 로그아웃(세션 폐기) → 204 | 회원 | AUTH-02 |
| `GET /api/me` | `{ id, nickname, profileImageUrl, role, blogAddress|null }`, 비회원은 401 | 회원 | AUTH-01, AUTH-04 |
| `PATCH /api/me` | `{ nickname?, profileImageId? }` | 회원 | AUTH-05 |
| `DELETE /api/me` | 탈퇴(확인 문구 `{ confirm: "탈퇴" }`) | 회원 | AUTH-06 |

## BLOG 블로그

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `GET /api/blogs/address-check?address=my-dog` | `{ available: bool, reason: "FORMAT" \| "RESERVED" \| "TAKEN" \| null }` | 회원 | BLOG-01-2 |
| `POST /api/blogs` | `{ address, name, description? }` → 201 `BlogInfo`. 이미 블로그가 있으면 409 | 회원 | BLOG-01 |
| `GET /api/blogs/{address}` | `BlogInfo`. 없음·삭제·이용 제한(남에게)은 404 | 모두 | BLOG-02, BLOG-03 |
| `PATCH /api/blogs/{address}` | `{ name?, description?, profileImageId?, topicId? }`. `address`는 받지 않음 | 주인 | BLOG-02, POST-11 |
| `GET /api/blogs/{address}/sidebar` | `{ blog: BlogInfo, totalPostCount, categories: [{ id, name, postCount, children: [...] }], uncategorizedCount }` (보는 사람 기준 개수) | 모두 | BLOG-04 |
| `DELETE /api/blogs/{address}` | 블로그 삭제(주소는 영구 예약) | 주인 | BLOG-07 |

## POST 글

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `GET /api/blogs/{address}/posts?page=&categoryId=&tag=` | 블로그 글 목록(페이지). `categoryId=uncategorized` 가능. 상위 카테고리는 하위 포함 | 모두 | BLOG-03, CAT-02, TAG-02 |
| `GET /api/blogs/{address}/posts/{slug}` | `PostDetail`. 볼 수 없으면 404. 조회수 기록(30분 중복 제외) | 모두 | POST-04, POST-09, POST-10 |
| `POST /api/blogs/{address}/posts` | 새 글 발행 또는 임시저장. `{ title, contentMarkdown, categoryId?, tags: [string], visibility, status: "PUBLISHED" \| "DRAFT" \| "SCHEDULED", scheduledAt?, thumbnailImageId?, draftId? }` → 201 `{ id, slug }`. 발행 검증은 제목 → 본문 순으로 첫 오류만 | 주인 | POST-01, POST-06, POST-08, POST-13, TAG-01 |
| `PUT /api/blogs/{address}/posts/{id}` | 수정(같은 본문 형식). slug·publishedAt은 바뀌지 않음 | 주인 | POST-02 |
| `PATCH /api/blogs/{address}/posts/{id}/visibility` | `{ visibility }` | 주인 | POST-06 |
| `DELETE /api/blogs/{address}/posts/{id}` | 글과 딸린 데이터 삭제(트랜잭션) → 204 | 주인 | POST-03 |
| `GET /api/blogs/{address}/drafts` | 임시저장 글 목록 | 주인 | POST-08 |
| `POST /api/images` | `multipart: file` → 201 `{ id, url, thumbUrl, width, height }`. 415 이미지 아님, 413 10MB 초과 | 회원 | POST-05, POL-06 |

## CAT 카테고리 · TAG 태그

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `POST /api/blogs/{address}/categories` | `{ name, parentId? }`. 같은 단계 이름 중복 409, 2단계 넘으면 400 | 주인 | CAT-01, CAT-03 |
| `PATCH /api/blogs/{address}/categories/{id}` | `{ name?, isPrivate? }` | 주인 | CAT-01, CAT-05 |
| `PUT /api/blogs/{address}/categories/order` | `{ orderedIds: [..] }` (같은 단계 안 순서) | 주인 | CAT-04 |
| `DELETE /api/blogs/{address}/categories/{id}` | 소속 글은 미분류로. 하위가 있으면 409 | 주인 | CAT-01, POL-07-3 |
| `GET /api/blogs/{address}/tags` | `[{ name, postCount }]` (볼 수 있는 글 기준) | 모두 | TAG-03 |
| `PATCH /api/blogs/{address}/tags/{name}` / `DELETE …` | 태그 이름 변경·삭제(글은 남음) | 주인 | TAG-04 |

## CMT 댓글·방명록

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `GET /api/posts/{postId}/comments` | 작성순 `[Comment]` + `count` | 글을 볼 수 있는 사람 | CMT-01 |
| `POST /api/posts/{postId}/comments` | `{ content, parentId?, secret? }` → 201 `Comment`. 비회원 401, 댓글 막힌 글·차단 회원 403 | 회원 | CMT-01, CMT-05~07, MNG-04 |
| `PATCH /api/comments/{id}` | `{ content }` | 작성자 | CMT-03 |
| `DELETE /api/comments/{id}` | 작성자 또는 블로그 주인 → 204 | 작성자·주인 | CMT-01, CMT-02 |
| `GET /api/blogs/{address}/guestbook?page=` / `POST …` / `DELETE /api/guestbook/{id}` | 방명록 목록·쓰기·삭제(댓글과 같은 규칙) | 모두 / 회원 / 작성자·주인 | CMT-04 |

## SOC 반응 · SUB 구독

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `PUT /api/posts/{postId}/like` / `DELETE …` | 공감 켜기·끄기(여러 번 와도 결과 같음) → `{ liked, likeCount }`. 자기 글 403 | 회원 | SOC-01 |
| `PUT /api/posts/{postId}/save` / `DELETE …` | 저장 켜기·끄기 | 회원 | SOC-03 |
| `GET /api/me/saved?page=` | 저장한 글(볼 수 있는 것만) | 회원 | SOC-03 |
| `PUT /api/blogs/{address}/subscription` / `DELETE …` | 구독·해제 → `{ subscribed, subscriberCount, mutual }`. 자기 블로그 400 | 회원 | SUB-01, SUB-03, SUB-05 |
| `GET /api/me/subscriptions` | 내가 구독한 블로그 목록 | 회원 | SUB-01 |
| `GET /api/feed?cursor=` | 구독 피드(커서) | 회원 | SUB-02 |
| `GET /api/notifications?cursor=` / `POST /api/notifications/{id}/read` / `GET /api/notifications/unread-count` | 알림 | 회원 | SUB-04 |
| `GET /api/recommendations/blogs` | 추천 블로그 | 회원 | SUB-06 |

## SRCH 검색 · HOME 홈

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `GET /api/blogs/{address}/search?q=&page=` | 블로그 내 검색. `q`를 trim해 비면 400 `EMPTY_QUERY` | 모두 | SRCH-01 |
| `GET /api/search?q=&type=post\|blog&page=` | 통합 검색 | 모두 | SRCH-02 |
| `GET /api/home/latest?cursor=` | 홈 최신 글(커서) | 모두 | HOME-01 |
| `GET /api/home/popular?limit=10` | 인기 글(최근 7일 공감+댓글, 10분 캐시) | 모두 | HOME-02, HOME-05 |
| `GET /api/topics` / `GET /api/topics/{id}/posts?cursor=` | 주제 목록, 주제별 글 | 모두 | HOME-03 |
| `GET /api/home/popular-blogs?limit=` | 인기 블로거 | 모두 | HOME-04 |

## MNG 블로그 관리

| 메서드·경로 | 설명 | 권한 | 기능 |
| --- | --- | --- | --- |
| `GET /api/manage/{address}/posts?status=&visibility=&page=` | 내 글 관리(임시저장·숨김 상태와 사유 포함) | 주인 | MNG-01 |
| `GET /api/manage/{address}/comments?page=` | 내 블로그 댓글·방명록 모아 보기(어느 글인지 포함) | 주인 | MNG-02 |
| `GET /api/manage/{address}/stats` | 오늘·어제·전체 방문자, 기간 인기 글 | 주인 | MNG-03 |
| `PUT/DELETE /api/manage/{address}/blocked-users/{userId}`, `GET/POST/DELETE /api/manage/{address}/banned-words` | 차단·금칙어 | 주인 | MNG-04 |

## ADMIN 서비스 관리 (모두 서비스 관리자만, 일반 회원 403·비회원 401)

| 메서드·경로 | 설명 | 기능 |
| --- | --- | --- |
| `GET /api/admin/ping` | 관리 영역 접근 확인 → 204 | ADMIN-01 |
| `POST /api/admin/users/{id}/sanctions` | `{ reason, endsAt|null }` 회원 이용 제한(관리자 대상 403) | ADMIN-02 |
| `POST /api/admin/sanctions/{id}/release` | 미리 해제 | ADMIN-02 |
| `POST /api/admin/posts/{id}/hide` / `…/unhide`, `POST /api/admin/comments/{id}/hide` / `…/unhide` | `{ reason }` 숨김·해제 | ADMIN-03 |
| `POST /api/reports` (회원) / `GET /api/admin/reports?status=` / `POST /api/admin/reports/{id}/resolve` | 신고와 처리(`{ action: "HIDE" \| "REJECT" }`) | ADMIN-04 |
| `POST /api/admin/blogs/{address}/restrict` / `…/unrestrict` | 블로그 이용 제한 | ADMIN-05 |
| `POST /api/notices` (관리자) / `GET /api/notices` (모두), `GET /api/admin/logs`, `GET /api/admin/dashboard` | 공지, 이력(읽기 전용), 현황 | ADMIN-06 |

## 계약 테스트 기준

- 위 표의 "권한" 열은 MockMvc 테스트에서 비회원·회원·주인·관리자 네 상태로 모두 확인한다(SC-003).
- 남의 비공개·구독자 공개 글은 목록·검색·홈·피드·사이드바 개수 응답에 나타나지 않아야 한다(SC-004).
- 같은 `Idempotency-Key`로 10번 보낸 발행·댓글 요청은 1건만 만든다(SC-005).
