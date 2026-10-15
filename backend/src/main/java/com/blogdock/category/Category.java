package com.blogdock.category;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_id", nullable = false, updatable = false)
    private Long blogId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** HOME-05 이 카테고리로 글을 쓸 때 미리 골라 둘 테마 (없으면 null). */
    private String theme;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Category() {
    }

    public static Category create(Long blogId, String name, int sortOrder) {
        Category c = new Category();
        c.blogId = blogId;
        c.name = name;
        c.sortOrder = sortOrder;
        c.createdAt = Instant.now();
        return c;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void changeTheme(String theme) {
        this.theme = theme;
    }

    public String getTheme() {
        return theme;
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
