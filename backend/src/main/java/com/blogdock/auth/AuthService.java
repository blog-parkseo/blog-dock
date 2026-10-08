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
