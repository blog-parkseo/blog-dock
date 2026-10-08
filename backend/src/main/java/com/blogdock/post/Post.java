package com.blogdock.post;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post")
public class Post {

    public static final String PUBLIC = "PUBLIC";
    public static final String PRIVATE = "PRIVATE";
    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(name = "category_id")
    private Long categoryId;

    /** 글 주소. 처음 발행할 때 정하고 바꾸지 않는다. */
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_markdown", nullable = false)
    private String contentMarkdown;

    /** 서버에서 정화한 HTML. 화면은 이것만 그린다. */
    @Column(name = "content_html", nullable = false)
    private String contentHtml;

    /** 태그를 뺀 글자. 요약과 검색에 쓴다. */
    @Column(name = "content_text", nullable = false)
    private String contentText;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(nullable = false)
    private String visibility;

    @Column(nullable = false)
    private String status;

    /** 처음 발행한 시각. 목록 순서의 기준이고 수정해도 바뀌지 않는다. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Post() {
    }

    public static Post create(Long blogId) {
        Post p = new Post();
        p.blogId = blogId;
        p.status = DRAFT;
        p.visibility = PUBLIC;
        p.title = "";
        p.contentMarkdown = "";
        p.contentHtml = "";
        p.contentText = "";
        p.createdAt = Instant.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    public void write(String title, Long categoryId, String visibility, PostContent content, String thumbnailUrl) {
        this.title = title;
        this.categoryId = categoryId;
        this.visibility = visibility;
        this.contentMarkdown = content.markdown();
        this.contentHtml = content.html();
        this.contentText = content.text();
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = Instant.now();
    }

    /** 임시저장 글을 발행하면 그때가 발행일이 된다. 이미 발행한 글은 그대로. */
    public void publish() {
        if (!isPublished()) {
            this.status = PUBLISHED;
            this.publishedAt = Instant.now();
            this.slug = String.valueOf(id);
        }
    }

    public void changeVisibility(String visibility) {
        this.visibility = visibility;
        this.updatedAt = Instant.now();
    }

    public boolean isPublished() {
        return PUBLISHED.equals(status);
    }

    public boolean isPublic() {
        return PUBLIC.equals(visibility);
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public String getContentText() {
        return contentText;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
