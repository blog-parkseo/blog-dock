package com.blogdock.reaction;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** 공감. 회원·글마다 하나만 저장된다 (복합 기본 키). */
@Entity
@Table(name = "post_like")
@IdClass(PostLike.Key.class)
public class PostLike {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostLike() {
    }

    public PostLike(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
        this.createdAt = Instant.now();
    }

    public record Key(Long userId, Long postId) implements Serializable {
        public Key() {
            this(null, null);
        }
    }
}
