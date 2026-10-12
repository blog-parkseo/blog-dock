package com.blogdock.auth;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.common.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final AuthService authService;
    private final LoginSession loginSession;
    private final ObjectProvider<ClientRegistrationRepository> kakao;
    private final boolean devLoginEnabled;
    private final boolean devLoginAdmin;

    public AuthController(AuthService authService, LoginSession loginSession,
                          ObjectProvider<ClientRegistrationRepository> kakao,
                          @Value("${blogdock.dev-login.enabled:true}") boolean devLoginEnabled,
                          @Value("${blogdock.dev-login.admin:true}") boolean devLoginAdmin) {
        this.authService = authService;
        this.loginSession = loginSession;
        this.kakao = kakao;
        this.devLoginEnabled = devLoginEnabled;
        this.devLoginAdmin = devLoginAdmin;
    }

    /**
     * 닉네임만 치는 가짜 로그인. 카카오 키 없이도 쓸 수 있다.
     * blogdock.dev-login.enabled=false 이면 꺼진다. dev-login.admin=false 이면 admin 계정으로는 못 들어간다.
     */
    @PostMapping("/api/auth/dev-login")
    public MeResponse devLogin(@Valid @RequestBody DevLoginRequest req,
                               HttpServletRequest request, HttpServletResponse response) {
        if (!devLoginEnabled) {
            throw ApiException.notFound("닉네임 로그인이 꺼져 있어요");
        }
        if (!devLoginAdmin && "admin".equals(req.nickname().trim())) {
            throw ApiException.forbidden("이 닉네임으로는 로그인할 수 없어요");
        }
        Member member = authService.devLogin(req.nickname());
        loginSession.save(member, request, response);
        return authService.me(member.getId());
    }

    /** 로그인 화면이 어떤 버튼을 보여 줄지. */
    @GetMapping("/api/auth/providers")
    public Map<String, Boolean> providers() {
        return Map.of(
                "kakao", kakao.getIfAvailable() != null,
                "dev", devLoginEnabled);
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
