package com.blogdock.auth;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 로그인한 회원을 세션에 기억시킨다. 가짜 로그인과 카카오 로그인이 같이 쓴다. */
@Component
public class LoginSession {

    private final SecurityContextRepository securityContextRepository;

    public LoginSession(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public void save(Member member, HttpServletRequest request, HttpServletResponse response) {
        LoginMember login = new LoginMember(member.getId(), member.getNickname(), member.getRole());
        var authentication = new UsernamePasswordAuthenticationToken(
                login, null, List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole())));
        // 로그인 전 세션을 버리고 새 세션 ID를 받는다 (세션 고정 공격 방지)
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
