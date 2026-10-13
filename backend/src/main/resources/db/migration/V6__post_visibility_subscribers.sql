-- POST-12 구독자 공개 글: 공개 범위에 SUBSCRIBERS를 더한다. 칸 길이(VARCHAR(12))는 그대로 충분하다.
ALTER TABLE post DROP CONSTRAINT ck_post_visibility;
ALTER TABLE post ADD CONSTRAINT ck_post_visibility CHECK (visibility IN ('PUBLIC', 'SUBSCRIBERS', 'PRIVATE'));
