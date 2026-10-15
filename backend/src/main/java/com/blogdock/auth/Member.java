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

    @Column(name = "social_provider")
    private String socialProvider;

    @Column(name = "social_id")
    private String socialId;

    /** 아이디로 가입한 회원의 아이디. 카카오 회원은 비어 있다. */
    @Column(name = "login_id")
    private String loginId;

    /** BCrypt로 바꾼 비밀번호. 원래 비밀번호는 저장하지 않는다. */
    @Column(name = "password_hash")
    private String passwordHash;

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

    /** 아이디·비밀번호로 가입. 항상 일반 회원이다. */
    public static Member joinWithPassword(String loginId, String passwordHash, String nickname) {
        Member m = new Member();
        m.loginId = loginId;
        m.passwordHash = passwordHash;
        m.nickname = nickname;
        m.role = "MEMBER";
        m.createdAt = Instant.now();
        return m;
    }

    /** 연결 해제한 소셜 계정이 가입할 때 쓴 계정이면 그 칸을 비운다. */
    public void clearSocial(String provider) {
        if (provider.equals(this.socialProvider)) {
            this.socialProvider = null;
            this.socialId = null;
        }
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
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
