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

    /** 답글이면 원래 댓글 id. 답글에는 다시 답글을 달지 않는다 (한 단계만). */
    @Column(name = "parent_id", updatable = false)
    private Long parentId;

    @Column(nullable = false)
    private String content;

    /** 비밀 댓글: 글 주인과 댓글 쓴 사람만 내용을 본다. */
    @Column(nullable = false)
    private boolean secret;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Comment() {
    }

    public static Comment write(Long postId, Long authorId, String content, boolean secret) {
        Comment c = new Comment();
        c.secret = secret;
        c.postId = postId;
        c.authorId = authorId;
        c.content = content;
        c.createdAt = Instant.now();
        c.updatedAt = c.createdAt;
        return c;
    }

    public static Comment reply(Comment parent, Long authorId, String content, boolean secret) {
        Comment c = write(parent.postId, authorId, content, secret);
        c.parentId = parent.id;
        return c;
    }

    public void edit(String content, boolean secret) {
        this.content = content;
        this.secret = secret;
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

    public Long getParentId() {
        return parentId;
    }

    public boolean isReply() {
        return parentId != null;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public boolean isSecret() {
        return secret;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
