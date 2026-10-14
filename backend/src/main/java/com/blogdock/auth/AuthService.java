package com.blogdock.auth;

import java.time.Instant;
import java.util.List;

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
    private final MemberSocialRepository socialRepository;
    private final BlogRepository blogRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttempts loginAttempts;

    public AuthService(MemberRepository memberRepository, MemberSocialRepository socialRepository,
                       BlogRepository blogRepository, PasswordEncoder passwordEncoder, LoginAttempts loginAttempts) {
        this.memberRepository = memberRepository;
        this.socialRepository = socialRepository;
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

    /** 카카오·구글·깃허브 로그인: 처음이면 가입(항상 일반 회원), 있으면 그 회원 (AUTH-01). */
    @Transactional
    public Member socialLogin(String provider, String socialId, String nickname, String profileImageUrl) {
        String name = nickname == null || nickname.isBlank() ? switch (provider) { case "GOOGLE" -> "구글회원"; case "GITHUB" -> "깃허브회원"; default -> "카카오회원"; } : nickname.strip();
        if (name.length() > 30) {
            name = name.substring(0, 30);
        }
        String finalName = name;
        // 연결된 계정이 있으면 그 회원으로 들어간다 (계정 연결). 없으면 새로 가입하고 연결해 둔다
        Member m = socialRepository.findByProviderAndSocialId(provider, socialId)
                .flatMap(s -> memberRepository.findById(s.getMemberId()))
                .orElseGet(() -> {
                    Member joined = memberRepository.findBySocialProviderAndSocialId(provider, socialId)
                            .orElseGet(() -> memberRepository.save(Member.join(provider, socialId, finalName)));
                    socialRepository.save(MemberSocial.link(joined.getId(), provider, socialId));
                    return joined;
                });
        m.fillProfileImageIfEmpty(profileImageUrl);
        return m;
    }

    /**
     * 계정 연결: 로그인한 회원에 소셜 계정을 붙인다.
     * 그 계정이 이미 블로그가 없는 빈 회원에 붙어 있으면 지금 회원으로 옮긴다.
     * 블로그가 있는 다른 회원에 붙어 있으면 연결하지 않는다 (남의 블로그를 가져오지 않게).
     */
    @Transactional
    public LinkResult linkSocial(Long memberId, String provider, String socialId) {
        var existing = socialRepository.findByProviderAndSocialId(provider, socialId);
        if (existing.isPresent()) {
            MemberSocial s = existing.get();
            if (s.getMemberId().equals(memberId)) {
                return LinkResult.LINKED;
            }
            if (blogRepository.existsByOwnerId(s.getMemberId())) {
                return LinkResult.USED_BY_OTHER;
            }
            if (socialRepository.findByMemberIdAndProvider(memberId, provider).isPresent()) {
                return LinkResult.ALREADY_HAS_PROVIDER;
            }
            s.moveTo(memberId);
            return LinkResult.LINKED;
        }
        if (socialRepository.findByMemberIdAndProvider(memberId, provider).isPresent()) {
            return LinkResult.ALREADY_HAS_PROVIDER;
        }
        socialRepository.save(MemberSocial.link(memberId, provider, socialId));
        return LinkResult.LINKED;
    }

    public enum LinkResult { LINKED, USED_BY_OTHER, ALREADY_HAS_PROVIDER }

    @Transactional(readOnly = true)
    public Member member(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없어요"));
    }

    @Transactional(readOnly = true)
    public List<SocialAccount> socials(Long memberId) {
        return socialRepository.findByMemberIdOrderByCreatedAtAsc(memberId).stream()
                .map(s -> new SocialAccount(s.getProvider().toLowerCase(), s.getCreatedAt())).toList();
    }

    public record SocialAccount(String provider, Instant linkedAt) {
    }

    /** 연결 해제. 들어올 방법이 하나도 안 남으면 (비밀번호도 없고 연결도 하나뿐) 막는다. */
    @Transactional
    public void unlinkSocial(Long memberId, String provider) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("회원을 찾을 수 없어요"));
        MemberSocial s = socialRepository.findByMemberIdAndProvider(memberId, provider.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("연결된 계정이 아니에요"));
        if (m.getLoginId() == null && socialRepository.countByMemberId(memberId) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_LOGIN",
                    "마지막 로그인 방법이라 해제할 수 없어요. 다른 계정을 먼저 연결해 주세요", null);
        }
        socialRepository.delete(s);
        // 예전 방식(member 칸)으로 다시 찾아 연결되지 않게 그 칸도 비운다
        m.clearSocial(s.getProvider());
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
