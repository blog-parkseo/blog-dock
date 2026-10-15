-- 소셜 계정 연결: 한 회원에 카카오·구글·깃허브 계정을 여러 개 붙인다.
-- 어느 걸로 로그인해도 같은 회원(같은 블로그)으로 들어간다.
-- member.social_provider/social_id 는 그대로 둔다 (롤링 배포 중 예전 블로그가 계속 쓴다)
CREATE TABLE member_social (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    social_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_msocial_provider_sid UNIQUE (provider, social_id),
    CONSTRAINT uk_msocial_member_provider UNIQUE (member_id, provider),
    CONSTRAINT fk_msocial_member FOREIGN KEY (member_id) REFERENCES member (id)
);

-- 지금까지 소셜로 가입한 회원은 그 계정을 연결된 것으로 옮겨 둔다
INSERT INTO member_social (member_id, provider, social_id)
SELECT id, social_provider, social_id FROM member WHERE social_provider IN ('KAKAO', 'GOOGLE', 'GITHUB');
