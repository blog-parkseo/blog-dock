-- 1차 구현 테이블 11개 (Crowfoot 문서 671과 같은 구조)

CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    social_provider VARCHAR(20) NOT NULL DEFAULT 'KAKAO',
    social_id VARCHAR(64) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    profile_image_url VARCHAR(500),
    role VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_social_provider_social_id UNIQUE (social_provider, social_id),
    CONSTRAINT ck_member_role CHECK (role IN ('MEMBER', 'ADMIN'))
);

CREATE TABLE reserved_word (
    word VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (word)
);

CREATE TABLE blog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    address VARCHAR(32) NOT NULL,
    name VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_address UNIQUE (address),
    CONSTRAINT uk_blog_owner_id UNIQUE (owner_id),
    CONSTRAINT fk_blog_member FOREIGN KEY (owner_id) REFERENCES member (id)
);

CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blog_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_blog_id_name UNIQUE (blog_id, name),
    CONSTRAINT fk_category_blog FOREIGN KEY (blog_id) REFERENCES blog (id) ON DELETE CASCADE
);

CREATE TABLE post (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blog_id BIGINT NOT NULL,
    category_id BIGINT,
    slug VARCHAR(100),
    title VARCHAR(100) NOT NULL,
    content_markdown MEDIUMTEXT NOT NULL,
    content_html MEDIUMTEXT NOT NULL,
    content_text MEDIUMTEXT NOT NULL,
    thumbnail_url VARCHAR(500),
    visibility VARCHAR(12) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_blog_id_slug UNIQUE (blog_id, slug),
    CONSTRAINT ck_post_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_post_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT fk_post_blog FOREIGN KEY (blog_id) REFERENCES blog (id),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
);

CREATE TABLE tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blog_id BIGINT NOT NULL,
    name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_blog_id_name UNIQUE (blog_id, name),
    CONSTRAINT fk_tag_blog FOREIGN KEY (blog_id) REFERENCES blog (id) ON DELETE CASCADE
);

CREATE TABLE post_tag (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

CREATE TABLE image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uploader_id BIGINT NOT NULL,
    post_id BIGINT,
    original_path VARCHAR(300) NOT NULL,
    thumb_path VARCHAR(300) NOT NULL,
    mime_type VARCHAR(20) NOT NULL,
    size_bytes INT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_image_mime_type CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/gif', 'image/webp')),
    CONSTRAINT ck_image_size CHECK (size_bytes <= 10485760),
    CONSTRAINT fk_image_member FOREIGN KEY (uploader_id) REFERENCES member (id),
    CONSTRAINT fk_image_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE SET NULL
);

CREATE TABLE comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_member FOREIGN KEY (author_id) REFERENCES member (id)
);

CREATE TABLE post_like (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_post_like_member FOREIGN KEY (user_id) REFERENCES member (id),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

CREATE TABLE idempotency_record (
    user_id BIGINT NOT NULL,
    idem_key CHAR(36) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status INT,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, idem_key),
    CONSTRAINT fk_idempotency_record_member FOREIGN KEY (user_id) REFERENCES member (id)
);

CREATE INDEX idx_category_blog_id_sort_order ON category (blog_id, sort_order);
CREATE INDEX idx_post_blog_id_status_published_at_id ON post (blog_id, status, published_at DESC, id DESC);
CREATE INDEX idx_post_status_visibility_published_at_id ON post (status, visibility, published_at DESC, id DESC);
CREATE INDEX idx_post_category_id_status_published_at_id ON post (category_id, status, published_at DESC, id DESC);
CREATE INDEX idx_comment_post_id_created_at_id ON comment (post_id, created_at, id);
CREATE INDEX idx_idempotency_record_created_at ON idempotency_record (created_at);

-- 블로그 주소로 쓸 수 없는 단어
INSERT INTO reserved_word (word) VALUES
  ('www'), ('api'), ('admin'), ('login'), ('logout'), ('files'), ('static'), ('mail'),
  ('help'), ('blog'), ('oauth2'), ('manage'), ('search'), ('feed'), ('notice');

-- 시작할 때 있는 관리자 계정 (ADMIN-01). 가짜 로그인에서 닉네임 admin으로 들어간다
INSERT INTO member (social_provider, social_id, nickname, role) VALUES ('DEV', 'admin', 'admin', 'ADMIN');
