package com.blogdock.tag;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** 글과 태그를 잇는 표. 같은 글에 같은 태그는 한 번만 (복합 기본 키). */
@Entity
@Table(name = "post_tag")
@IdClass(PostTag.Key.class)
public class PostTag {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostTag() {
    }

    public PostTag(Long postId, Long tagId) {
        this.postId = postId;
        this.tagId = tagId;
        this.createdAt = Instant.now();
    }

    public record Key(Long postId, Long tagId) implements Serializable {
        public Key() {
            this(null, null);
        }
    }
}
