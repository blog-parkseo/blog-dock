package com.blogdock.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.BlogRepository;
import com.blogdock.common.ApiException;

@Service
public class AuthService {

    /** 없는 아이디일 때도 비교하는 시간을 똑같이 맞추려고 쓰는 가짜 암호문. */
    private static final String DUMMY_HASH = "$2a$10$dBhEIZUlN4/IylDsLbuwa.EmPCAlfRzuEQ4nifI5RhdslER3EcJMK";

    private final MemberRepository memberRepository;
    private final BlogRepository blogRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttempts loginAttempts;

    public AuthService(MemberRepository memberRepository, BlogRepository blogRepository,
                       PasswordEncoder passwordEncoder, LoginAttempts loginAttempts) {
        this.memberRepository = memberRepository;
        this.blogRepository = blogRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
    }

    /** AUTH-01 회원가입: 아이디가 겹치면 안 되고, 비밀번호는 BCrypt로 바꿔 저장한다. */
    @Transactional
    public Member signup(SignupRequest req) {
        if (memberRepository.existsByLoginId(req.loginId())) {
            throw new ApiException(HttpStatus.CONFLICT, "LOGIN_ID_TAKEN", "이미 사용 중인 아이디예요", "loginId");
        }
        return memberRepository.save(
                Member.joinWithPassword(req.loginId(), passwordEncoder.encode(req.password()), req.nickname().trim()));
    }

    /** AUTH-01 로그인: 아이디가 없거나 비밀번호가 틀리면 같은 문구로 알린다. 5번 틀리면 5분 동안 막는다. */
    @Transactional(readOnly = true)
    public Member login(LoginRequest req) {
        String loginId = req.loginId().trim();
        if (loginAttempts.isBlocked(loginId)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_BLOCKED",
                    "비밀번호를 여러 번 틀려서 잠시 막았어요. 5분 뒤에 다시 시도해 주세요", null);
        }
        Member member = memberRepository.findByLoginId(loginId).orElse(null);
        String hash = member == null || member.getPasswordHash() == null ? DUMMY_HASH : member.getPasswordHash();
        boolean matches = passwordEncoder.matches(req.password(), hash);
        if (member == null || member.getPasswordHash() == null || !matches) {
            loginAttempts.fail(loginId);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 맞지 않아요", null);
        }
        loginAttempts.succeed(loginId);
        return member;
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
