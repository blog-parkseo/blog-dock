package com.blogdock.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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

    private static final Logger log = LoggerFactory.getLogger(SocialLogin.class);

    static final String REDIRECT_KEY = "LOGIN_REDIRECT";
    static final String LINK_KEY = "LINK_MEMBER_ID";

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

    /** 계정 연결: 로그인한 회원 번호를 기억하고 소셜 로그인으로 보낸다. 돌아오면 로그인 대신 연결한다. */
    public static void rememberLink(HttpServletRequest request, Long memberId) {
        request.getSession(true).setAttribute(LINK_KEY, memberId);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SocialProfile p = profile((OAuth2AuthenticationToken) authentication);
        Long linkingMemberId = popLink(request);
        if (linkingMemberId != null) {
            var result = authService.linkSocial(linkingMemberId, p.provider(), p.socialId());
            // 소셜 로그인이 세션의 로그인 정보를 바꿔 놓았으니 연결하던 회원으로 되돌린다
            loginSession.save(authService.member(linkingMemberId), request, response);
            String query = switch (result) {
                case LINKED -> "linked=" + p.provider().toLowerCase();
                case USED_BY_OTHER -> "linkError=used";
                case ALREADY_HAS_PROVIDER -> "linkError=already";
            };
            response.sendRedirect("/settings?" + query);
            return;
        }
        Member member = authService.socialLogin(p.provider(), p.socialId(), p.name(), p.imageUrl());
        String redirect = popRedirect(request);
        loginSession.save(member, request, response);
        response.sendRedirect(redirect);
    }

    private record SocialProfile(String provider, String socialId, String name, String imageUrl) {
    }

    /** 회사마다 다른 사용자 정보에서 회원 번호, 이름, 사진을 꺼낸다. */
    @SuppressWarnings("unchecked")
    private static SocialProfile profile(OAuth2AuthenticationToken token) {
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        String registrationId = token.getAuthorizedClientRegistrationId();
        if ("google".equals(registrationId)) {
            // 구글: sub(회원 번호), name(이름), picture(사진)
            return new SocialProfile("GOOGLE", String.valueOf(attrs.get("sub")),
                    (String) attrs.get("name"), (String) attrs.get("picture"));
        }
        if ("github".equals(registrationId)) {
            // 깃허브: id(회원 번호), name(이름, 없으면 login 아이디), avatar_url(사진)
            String name = (String) attrs.get("name");
            return new SocialProfile("GITHUB", String.valueOf(attrs.get("id")),
                    name == null || name.isBlank() ? (String) attrs.get("login") : name, (String) attrs.get("avatar_url"));
        }
        Map<String, Object> account = (Map<String, Object>) attrs.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
        return new SocialProfile("KAKAO", String.valueOf(attrs.get("id")),
                (String) profile.get("nickname"), (String) profile.get("profile_image_url"));
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String uri = request.getRequestURI();
        String provider = uri.endsWith("/google") ? "google" : uri.endsWith("/github") ? "github" : "kakao";
        String reason = reason(exception);
        log.warn("{} 로그인 실패: {} ({})", provider, reason, exception.getMessage());
        if (popLink(request) != null) {
            response.sendRedirect("/settings?linkError=failed&reason=" + reason);
            return;
        }
        String redirect = popRedirect(request);
        response.sendRedirect("/login?error=" + provider + "&reason=" + reason
                + "&redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }

    /** 실패 원인 코드 (예: access_denied, authorization_request_not_found). 화면에 안내 문구를 고르는 데 쓴다. */
    static String reason(AuthenticationException exception) {
        String code = exception instanceof OAuth2AuthenticationException e ? e.getError().getErrorCode() : "unknown";
        return code != null && code.matches("[a-z_]{1,40}") ? code : "unknown";
    }

    private Long popLink(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(LINK_KEY);
        session.removeAttribute(LINK_KEY);
        return (Long) value;
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
