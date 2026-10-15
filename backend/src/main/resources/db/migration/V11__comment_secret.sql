-- 비밀 댓글: 글 주인과 댓글 쓴 사람만 내용을 본다.
-- 기본값이 있어서 롤링 배포 중 예전 블로그가 댓글을 써도 그대로 공개 댓글이 된다.
ALTER TABLE comment ADD COLUMN secret BOOLEAN NOT NULL DEFAULT FALSE;
