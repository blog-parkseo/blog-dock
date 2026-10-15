-- SUB-01~03 구독. 회원·블로그마다 하나만 저장된다.
-- 블로그가 지워지면 구독도 같이 지운다. 구독자 수를 셀 때 blog_id 인덱스를 쓴다.
CREATE TABLE subscription (
    user_id BIGINT NOT NULL,
    blog_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, blog_id),
    CONSTRAINT fk_subscription_member FOREIGN KEY (user_id) REFERENCES member (id),
    CONSTRAINT fk_subscription_blog FOREIGN KEY (blog_id) REFERENCES blog (id) ON DELETE CASCADE
);

CREATE INDEX idx_subscription_blog_id ON subscription (blog_id);
