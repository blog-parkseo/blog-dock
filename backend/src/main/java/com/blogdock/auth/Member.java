package com.blogdock.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "social_provider", nullable = false)
    private String socialProvider;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(nullable = false)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Member() {
    }

    /** 가입하면 항상 일반 회원(MEMBER)이다. */
    public static Member join(String socialProvider, String socialId, String nickname) {
        Member m = new Member();
        m.socialProvider = socialProvider;
        m.socialId = socialId;
        m.nickname = nickname;
        m.role = "MEMBER";
        m.createdAt = Instant.now();
        return m;
    }

    /** AUTH-05 닉네임·프로필 이미지 바꾸기. */
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    /** 카카오에서 받은 프로필 이미지는 내가 직접 바꾸지 않았을 때만 채운다. */
    public void fillProfileImageIfEmpty(String url) {
        if (this.profileImageUrl == null && url != null) {
            this.profileImageUrl = url;
        }
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getRole() {
        return role;
    }
}
