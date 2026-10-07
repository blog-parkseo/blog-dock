-- 티스토리형 블로그 — 추가 인덱스 (MySQL 8 / H2 MySQL 모드 공용)
-- Crowfoot 문서 656과 같은 내용이다. 기본 키·유니크 키·외래 키 인덱스는 테이블을 만들 때 자동으로 생기므로 여기에 없다.
-- 마이그레이션을 만들 때 테이블과 같은 파일에 넣는다: [V1] → DB/V1__core.sql, [V2] → DB/V2__p1.sql, [V3] → DB/V3__p2.sql

-- ===== [V1] P0 =====

-- 블로그 메인 글 목록·사이드바 글 수·구독 피드 (BLOG-03, SUB-02)
-- WHERE blog_id = ? AND status = 'PUBLISHED' ORDER BY published_at DESC, id DESC
CREATE INDEX idx_post_blog_id_status_published_at_id ON post (blog_id, status, published_at DESC, id DESC);

-- 홈 최신 글 (HOME-01)
-- WHERE status = 'PUBLISHED' AND visibility = 'PUBLIC' ORDER BY published_at DESC, id DESC
CREATE INDEX idx_post_status_visibility_published_at_id ON post (status, visibility, published_at DESC, id DESC);

-- 카테고리별 글 목록 (CAT-02)
-- WHERE category_id = ? AND status = 'PUBLISHED' ORDER BY published_at DESC, id DESC
CREATE INDEX idx_post_category_id_status_published_at_id ON post (category_id, status, published_at DESC, id DESC);

-- 글의 댓글 작성순 (CMT-01)
-- WHERE post_id = ? ORDER BY created_at, id
CREATE INDEX idx_comment_post_id_created_at_id ON comment (post_id, created_at, id);

-- 최근 7일 인기 글 집계 — 댓글 (HOME-02)
-- WHERE created_at >= ? GROUP BY post_id
CREATE INDEX idx_comment_created_at_post_id ON comment (created_at, post_id);

-- 글별 공감 수 (SOC-01)
CREATE INDEX idx_post_like_post_id_created_at ON post_like (post_id, created_at);

-- 최근 7일 인기 글 집계 — 공감 (HOME-02)
CREATE INDEX idx_post_like_created_at_post_id ON post_like (created_at, post_id);

-- 24시간 지난 중복 요청 기록 정리 (연달아 눌러도 한 번만 처리, POST-01)
-- DELETE ... WHERE created_at < ?
CREATE INDEX idx_idempotency_record_created_at ON idempotency_record (created_at);

-- ===== [V2] P1 =====

-- 같은 사람 30분 안 재조회 확인·조회수 (POST-09)
-- WHERE post_id = ? AND viewer_key = ? AND viewed_at >= ?
CREATE INDEX idx_post_view_log_post_id_viewer_key_viewed_at ON post_view_log (post_id, viewer_key, viewed_at);

-- 방명록 최신순 (CMT-04)
CREATE INDEX idx_guestbook_blog_id_created_at ON guestbook (blog_id, created_at DESC);

-- 사이드바 카테고리 순서 (CAT-04)
-- WHERE blog_id = ? ORDER BY sort_order
CREATE INDEX idx_category_blog_id_sort_order ON category (blog_id, sort_order);

-- ===== [V3] P2 =====

-- 예약 발행 작업이 공개할 글 찾기 (POST-13)
-- WHERE status = 'SCHEDULED' AND scheduled_at <= NOW()
CREATE INDEX idx_post_status_scheduled_at ON post (status, scheduled_at);

-- 내가 저장한 글 최신순 (SOC-03)
-- WHERE user_id = ? ORDER BY created_at DESC
CREATE INDEX idx_post_save_user_id_created_at ON post_save (user_id, created_at DESC);

-- 읽지 않은 알림 (SUB-04)
-- WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC
CREATE INDEX idx_notification_user_id_is_read_created_at ON notification (user_id, is_read, created_at DESC);

-- 대기 중인 신고 목록 (ADMIN-04)
-- WHERE status = 'PENDING' ORDER BY created_at
CREATE INDEX idx_report_status_created_at ON report (status, created_at);

-- 한 대상(글·댓글)에 쌓인 신고 모아 보기·함께 처리 (ADMIN-04)
-- WHERE target_type = ? AND target_id = ?
CREATE INDEX idx_report_target_type_target_id ON report (target_type, target_id);

-- ===== 나중에 (운영 MySQL 전용) =====
-- 글이 많아져 본문 검색(SRCH-01·02)의 LIKE가 느려지면 별도 마이그레이션으로 더한다.
-- H2는 FULLTEXT 문법을 지원하지 않으므로 개발 DB 마이그레이션에는 넣지 않는다.
-- CREATE FULLTEXT INDEX ftx_post_title_content_text ON post (title, content_text) WITH PARSER ngram;
