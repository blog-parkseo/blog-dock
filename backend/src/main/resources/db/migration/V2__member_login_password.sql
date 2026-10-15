-- AUTH-01 추가: 아이디·비밀번호 가입·로그인 (카카오 로그인과 같이 쓴다)
-- 아이디로 가입한 회원은 소셜 칸이 비어 있으므로 비워 둘 수 있게 바꾼다
ALTER TABLE member MODIFY social_provider VARCHAR(20) NULL;
ALTER TABLE member MODIFY social_id VARCHAR(64) NULL;

ALTER TABLE member ADD COLUMN login_id VARCHAR(20);
ALTER TABLE member ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE member ADD CONSTRAINT uk_member_login_id UNIQUE (login_id);

-- 시작할 때 있는 관리자 계정 (ADMIN-01)의 아이디는 admin.
-- 비밀번호는 여기 쓰지 않고, 서버가 켜질 때 ADMIN_PASSWORD 값으로 넣는다 (AdminPasswordInitializer)
UPDATE member SET login_id = 'admin' WHERE social_provider = 'DEV' AND social_id = 'admin';
