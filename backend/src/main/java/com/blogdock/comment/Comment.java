package com.blogdock.comment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Comment() {
    }

    public static Comment write(Long postId, Long authorId, String content) {
        Comment c = new Comment();
        c.postId = postId;
        c.authorId = authorId;
        c.content = content;
        c.createdAt = Instant.now();
        c.updatedAt = c.createdAt;
        return c;
    }

    public void edit(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public boolean isWrittenBy(Long memberId) {
        return authorId.equals(memberId);
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
