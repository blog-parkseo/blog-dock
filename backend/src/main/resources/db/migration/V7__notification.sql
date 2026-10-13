-- NOTI-01~03 알림. 받는 사람이 탈퇴하면 같이 지운다.
-- 글·블로그가 지워져도 알림은 남기고 "볼 수 없는 글이에요"를 보여 주려고 post_id, blog_id에는 외래 키를 걸지 않는다.
-- 보낸 사람 닉네임과 글 제목은 만들 때 적어 둔다 (나중에 글이 지워져도 문구가 남는다).
CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT,
    type VARCHAR(12) NOT NULL,
    blog_id BIGINT,
    post_id BIGINT,
    actor_name VARCHAR(30),
    blog_name VARCHAR(40),
    post_title VARCHAR(100),
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT ck_notification_type CHECK (type IN ('COMMENT', 'LIKE', 'SUBSCRIBE', 'NEW_POST'))
);

-- 내 알림 최신순 목록, 읽지 않은 수, 90일 지난 알림 지우기
CREATE INDEX idx_notification_recipient_id_id ON notification (recipient_id, id);
CREATE INDEX idx_notification_recipient_id_read_at ON notification (recipient_id, read_at);
CREATE INDEX idx_notification_created_at ON notification (created_at);
