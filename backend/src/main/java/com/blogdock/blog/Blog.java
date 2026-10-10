package com.blogdock.blog;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blog")
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    /** 주소는 바꿀 수 없다 (updatable = false). */
    @Column(nullable = false, updatable = false)
    private String address;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Blog() {
    }

    public static Blog open(Long ownerId, String address, String name, String description) {
        Blog b = new Blog();
        b.ownerId = ownerId;
        b.address = address;
        b.name = name;
        b.description = description;
        b.createdAt = Instant.now();
        return b;
    }

    public void update(String name, String description, String profileImageUrl) {
        this.name = name;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isOwnedBy(Long memberId) {
        return ownerId.equals(memberId);
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}
