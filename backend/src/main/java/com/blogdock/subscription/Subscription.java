package com.blogdock.subscription;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** 구독. 회원·블로그마다 하나만 저장된다 (복합 기본 키). */
@Entity
@Table(name = "subscription")
@IdClass(Subscription.Key.class)
public class Subscription {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "blog_id")
    private Long blogId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Subscription() {
    }

    public Subscription(Long userId, Long blogId) {
        this.userId = userId;
        this.blogId = blogId;
        this.createdAt = Instant.now();
    }

    public Long getBlogId() {
        return blogId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public record Key(Long userId, Long blogId) implements Serializable {
        public Key() {
            this(null, null);
        }
    }
}
