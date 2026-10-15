-- 답글: 댓글 밑에 한 단계만 단다. 원래 댓글이 지워지면 답글도 같이 지워진다.
-- 예전 블로그(롤링 배포 중)는 parent_id 를 모르지만, 비어 있어도 되는 칸이라 그대로 돌아간다.
ALTER TABLE comment ADD COLUMN parent_id BIGINT NULL;
ALTER TABLE comment ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (id) ON DELETE CASCADE;

-- 내 댓글에 답글이 달리면 알림
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type
    CHECK (type IN ('COMMENT', 'LIKE', 'SUBSCRIBE', 'NEW_POST', 'MUTUAL', 'REPLY'));
