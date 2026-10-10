package com.blogdock.auth;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class KakaoController {

    /** 보던 화면을 기억하고 카카오 로그인으로 보낸다. 카카오 키가 없으면 로그인 화면이 안내한다. */
    @GetMapping("/api/auth/kakao")
    public void start(@RequestParam(required = false) String redirect,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        KakaoLogin.remember(request, redirect);
        response.sendRedirect("/oauth2/authorization/kakao");
    }
}
