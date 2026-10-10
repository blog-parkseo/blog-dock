package com.blogdock.auth;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final AuthService authService;
    private final LoginSession loginSession;
    private final ObjectProvider<ClientRegistrationRepository> kakao;
    private final org.springframework.core.env.Environment env;

    public AuthController(AuthService authService, LoginSession loginSession,
                          ObjectProvider<ClientRegistrationRepository> kakao,
                          org.springframework.core.env.Environment env) {
        this.authService = authService;
        this.loginSession = loginSession;
        this.kakao = kakao;
        this.env = env;
    }

    /**
     * 개발용 가짜 로그인. 카카오 키 없이도 화면을 확인할 수 있게 남겨 둔다.
     * 운영(prod) 프로필에서는 이 주소가 아예 생기지 않는다.
     */
    @Profile("!prod")
    @PostMapping("/api/auth/dev-login")
    public MeResponse devLogin(@Valid @RequestBody DevLoginRequest req,
                               HttpServletRequest request, HttpServletResponse response) {
        Member member = authService.devLogin(req.nickname());
        loginSession.save(member, request, response);
        return authService.me(member.getId());
    }

    /** 로그인 화면이 어떤 버튼을 보여 줄지. */
    @GetMapping("/api/auth/providers")
    public Map<String, Boolean> providers() {
        return Map.of(
                "kakao", kakao.getIfAvailable() != null,
                "dev", !env.matchesProfiles("prod"));
    }

    /** 지금 로그인한 사람. 비회원이면 SecurityConfig가 401을 돌려준다. */
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal LoginMember login) {
        return authService.me(LoginMember.require(login));
    }

    /** AUTH-05 회원정보 수정. */
    @PatchMapping("/api/me")
    public MeResponse updateMe(@AuthenticationPrincipal LoginMember login, @Valid @RequestBody ProfileRequest req) {
        return authService.updateProfile(LoginMember.require(login), req.nickname(), req.profileImageUrl());
    }
}
