package com.blogdock.auth;

import java.io.IOException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class SocialController {

    /** 보던 화면을 기억하고 카카오·구글·깃허브 로그인으로 보낸다. 키가 없으면 로그인 화면이 버튼을 숨긴다. */
    @GetMapping("/api/auth/{provider:kakao|google|github}")
    public void start(@PathVariable String provider, @RequestParam(required = false) String redirect,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        SocialLogin.remember(request, redirect);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    /** 계정 연결: 로그인한 채로 다른 소셜 계정을 붙이러 간다. 로그인 안 했으면 로그인 화면으로. */
    @GetMapping("/api/auth/{provider:kakao|google|github}/link")
    public void link(@PathVariable String provider, @AuthenticationPrincipal LoginMember login,
                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (login == null) {
            response.sendRedirect("/login?redirect=%2Fsettings");
            return;
        }
        SocialLogin.rememberLink(request, login.id());
        response.sendRedirect("/oauth2/authorization/" + provider);
    }
}
