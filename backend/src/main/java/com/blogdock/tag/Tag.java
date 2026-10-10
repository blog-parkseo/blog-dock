package com.blogdock.tag;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 태그는 블로그마다 따로 있다. */
@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tag() {
    }

    public static Tag create(Long blogId, String name) {
        Tag t = new Tag();
        t.blogId = blogId;
        t.name = name;
        t.createdAt = Instant.now();
        return t;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
