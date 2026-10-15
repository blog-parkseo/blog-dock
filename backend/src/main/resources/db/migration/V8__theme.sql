-- HOME-05 테마: 사이트 공통 분류. 글마다 하나(없어도 된다), 카테고리마다 기본 테마 하나(없어도 된다).
-- 값은 코드(DAILY 등)로 저장하고 화면 이름은 코드에서 정한다. 이름을 바꿔도 데이터는 그대로다.
ALTER TABLE post ADD COLUMN theme VARCHAR(12) NULL;
ALTER TABLE post ADD CONSTRAINT ck_post_theme CHECK (theme IN ('DAILY', 'TRAVEL', 'FOOD', 'IT', 'BOOK_MOVIE'));
ALTER TABLE category ADD COLUMN theme VARCHAR(12) NULL;
ALTER TABLE category ADD CONSTRAINT ck_category_theme CHECK (theme IN ('DAILY', 'TRAVEL', 'FOOD', 'IT', 'BOOK_MOVIE'));

-- 홈 테마 피드: 테마별 공개 글을 최신순으로
CREATE INDEX idx_post_theme_status_visibility_published_at_id ON post (theme, status, visibility, published_at DESC, id DESC);
