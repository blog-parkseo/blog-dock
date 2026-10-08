package com.blogdock.auth;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * 개발용 가짜 로그인 (AUTH-01의 쉬운 버전). 5단계에서 카카오 로그인으로 바꾼다.
     * 운영(prod) 프로필에서는 이 주소가 아예 생기지 않는다.
     */
    @Profile("!prod")
    @PostMapping("/api/auth/dev-login")
    public MeResponse devLogin(@Valid @RequestBody DevLoginRequest req,
                               HttpServletRequest request, HttpServletResponse response) {
        Member member = authService.devLogin(req.nickname());
        LoginMember login = new LoginMember(member.getId(), member.getNickname(), member.getRole());

        var authentication = new UsernamePasswordAuthenticationToken(
                login, null, List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);  // 세션에 저장

        return authService.me(member.getId());
    }

    /** 지금 로그인한 사람. 비회원이면 SecurityConfig가 401을 돌려준다. */
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal LoginMember login) {
        return authService.me(login.id());
    }
}
