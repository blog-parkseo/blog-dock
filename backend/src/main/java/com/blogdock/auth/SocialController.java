package com.blogdock.auth;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class SocialController {

    /** 보던 화면을 기억하고 카카오·구글 로그인으로 보낸다. 키가 없으면 로그인 화면이 버튼을 숨긴다. */
    @GetMapping("/api/auth/{provider:kakao|google}")
    public void start(@PathVariable String provider, @RequestParam(required = false) String redirect,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        SocialLogin.remember(request, redirect);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }
}
