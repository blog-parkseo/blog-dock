-- SUB-07 예약 맞구독: 블로그가 없는 사람에게 맞구독을 눌러 두면, 그 사람이 블로그를 만들 때 자동으로 구독한다.
-- 예약한 사람이나 예약된 사람이 탈퇴하면 예약도 지운다.
CREATE TABLE subscription_reservation (
    user_id BIGINT NOT NULL,
    target_member_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, target_member_id),
    CONSTRAINT fk_reservation_member FOREIGN KEY (user_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_target FOREIGN KEY (target_member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_subscription_reservation_target_member_id ON subscription_reservation (target_member_id);

-- 예약이 이루어지면 예약한 사람에게 "블로그를 만들어서 맞구독이 됐어요" 알림을 보낸다
ALTER TABLE notification DROP CONSTRAINT ck_notification_type;
ALTER TABLE notification ADD CONSTRAINT ck_notification_type
    CHECK (type IN ('COMMENT', 'LIKE', 'SUBSCRIBE', 'NEW_POST', 'MUTUAL'));
