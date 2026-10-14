package com.blogdock.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 회원에 연결된 소셜 계정 하나 (카카오·구글·깃허브). 한 회원에 회사마다 하나씩 붙는다. */
@Entity
@Table(name = "member_social")
public class MemberSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private String provider;

    @Column(name = "social_id", nullable = false, updatable = false)
    private String socialId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MemberSocial() {
    }

    public static MemberSocial link(Long memberId, String provider, String socialId) {
        MemberSocial s = new MemberSocial();
        s.memberId = memberId;
        s.provider = provider;
        s.socialId = socialId;
        s.createdAt = Instant.now();
        return s;
    }

    /** 블로그가 없는 빈 회원에 붙어 있던 계정을 지금 회원으로 옮긴다. */
    public void moveTo(Long memberId) {
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
