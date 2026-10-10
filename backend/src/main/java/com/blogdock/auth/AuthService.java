package com.blogdock.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.BlogRepository;
import com.blogdock.common.ApiException;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final BlogRepository blogRepository;

    public AuthService(MemberRepository memberRepository, BlogRepository blogRepository) {
        this.memberRepository = memberRepository;
        this.blogRepository = blogRepository;
    }

    /** 가짜 로그인: 같은 닉네임이면 같은 회원, 처음이면 가입시킨다. */
    @Transactional
    public Member devLogin(String nickname) {
        String name = nickname.trim();
        return memberRepository.findBySocialProviderAndSocialId("DEV", name)
                .orElseGet(() -> memberRepository.save(Member.join("DEV", name, name)));
    }

    /** 카카오 로그인: 처음이면 가입(항상 일반 회원), 있으면 그 회원 (AUTH-01). */
    @Transactional
    public Member kakaoLogin(String kakaoId, String nickname, String profileImageUrl) {
        String name = nickname == null || nickname.isBlank() ? "카카오회원" : nickname.strip();
        if (name.length() > 30) {
            name = name.substring(0, 30);
        }
        String finalName = name;
        Member m = memberRepository.findBySocialProviderAndSocialId("KAKAO", kakaoId)
                .orElseGet(() -> memberRepository.save(Member.join("KAKAO", kakaoId, finalName)));
        m.fillProfileImageIfEmpty(profileImageUrl);
        return m;
    }

    /** AUTH-05 회원정보 수정. */
    @Transactional
    public MeResponse updateProfile(Long memberId, String nickname, String profileImageUrl) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없어요"));
        m.updateProfile(nickname.strip(), profileImageUrl == null || profileImageUrl.isBlank() ? null : profileImageUrl);
        return me(memberId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없어요"));
        String blogAddress = blogRepository.findByOwnerId(memberId)
                .map(b -> b.getAddress())
                .orElse(null);
        return new MeResponse(m.getId(), m.getNickname(), m.getProfileImageUrl(), m.getRole(), blogAddress);
    }
}
