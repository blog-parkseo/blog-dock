-- HOME-02 인기 글: 최근 7일 동안 받은 공감·댓글을 시각으로 골라 센다.
CREATE INDEX idx_post_like_created_at ON post_like (created_at);
CREATE INDEX idx_comment_created_at ON comment (created_at);
