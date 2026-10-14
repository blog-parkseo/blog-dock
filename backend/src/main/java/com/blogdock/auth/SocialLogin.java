package com.blogdock.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * AUTH-01 카카오·구글·깃허브 로그인.
 * 화면 → /api/auth/kakao?redirect=/보던화면 → 카카오 → /login/oauth2/code/kakao → 보던 화면. 구글·깃허브도 같다 (kakao 자리에 google, github).
 * 실패하거나 취소하면 아무것도 바꾸지 않고 /login?error=kakao (또는 google, github) 로 돌려보낸다.
 */
@Component
public class SocialLogin implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    static final String REDIRECT_KEY = "LOGIN_REDIRECT";

    private final AuthService authService;
    private final LoginSession loginSession;

    public SocialLogin(AuthService authService, LoginSession loginSession) {
        this.authService = authService;
        this.loginSession = loginSession;
    }

    /** 로그인 전에 보던 화면을 기억한다. 다른 사이트 주소는 받지 않는다. */
    public static String safeRedirect(String redirect) {
        if (redirect == null || !redirect.startsWith("/") || redirect.startsWith("//") || redirect.contains("\\")) {
            return "/";
        }
        return redirect;
    }

    public static void remember(HttpServletRequest request, String redirect) {
        request.getSession(true).setAttribute(REDIRECT_KEY, safeRedirect(redirect));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = user.getAttributes();
        Member member;
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        if ("google".equals(registrationId)) {
            // 구글: sub(회원 번호), name(이름), picture(사진)
            member = authService.socialLogin("GOOGLE", String.valueOf(attrs.get("sub")),
                    (String) attrs.get("name"), (String) attrs.get("picture"));
        } else if ("github".equals(registrationId)) {
            // 깃허브: id(회원 번호), name(이름, 없으면 login 아이디), avatar_url(사진)
            String name = (String) attrs.get("name");
            member = authService.socialLogin("GITHUB", String.valueOf(attrs.get("id")),
                    name == null || name.isBlank() ? (String) attrs.get("login") : name, (String) attrs.get("avatar_url"));
        } else {
            Map<String, Object> account = (Map<String, Object>) attrs.getOrDefault("kakao_account", Map.of());
            Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
            member = authService.socialLogin("KAKAO", String.valueOf(attrs.get("id")),
                    (String) profile.get("nickname"), (String) profile.get("profile_image_url"));
        }

        String redirect = popRedirect(request);
        loginSession.save(member, request, response);
        response.sendRedirect(redirect);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String uri = request.getRequestURI();
        String provider = uri.endsWith("/google") ? "google" : uri.endsWith("/github") ? "github" : "kakao";
        String redirect = popRedirect(request);
        response.sendRedirect("/login?error=" + provider + "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }

    private String popRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "/";
        }
        Object value = session.getAttribute(REDIRECT_KEY);
        session.removeAttribute(REDIRECT_KEY);
        return value == null ? "/" : (String) value;
    }
}
