-- 블로그 위쪽 배경. 비어 있으면 주소로 정하는 자동 색이다.
-- 그림: SUNSET(노을빛) MEADOW(풀빛) SKYFIELD(하늘빛) NIGHT(밤빛) / 색: SKY MINT LAVENDER LEMON ROSE GRAY
-- 예전 블로그(롤링 배포 중)는 이 칸을 모르지만, 비어 있어도 되는 칸이라 그대로 돌아간다.
ALTER TABLE blog ADD COLUMN cover VARCHAR(12) NULL;
ALTER TABLE blog ADD CONSTRAINT ck_blog_cover
    CHECK (cover IN ('SUNSET', 'MEADOW', 'SKYFIELD', 'NIGHT', 'SKY', 'MINT', 'LAVENDER', 'LEMON', 'ROSE', 'GRAY'));
