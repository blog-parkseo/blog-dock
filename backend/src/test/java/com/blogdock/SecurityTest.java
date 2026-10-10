package com.blogdock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.blogdock.auth.KakaoLogin;
import com.blogdock.auth.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

/** CSRF 방어: 쿠키의 토큰을 헤더로 돌려보내지 않으면 상태를 바꾸는 요청은 막힌다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sec;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void csrf_token_required_for_changes() throws Exception {
        mvc.perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"sec\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF"));

        String token = mvc.perform(get("/api/health")).andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();
        mvc.perform(post("/api/auth/dev-login").cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"sec\"}"))
                .andExpect(status().isOk());
    }

    @Autowired
    KakaoLogin kakaoLogin;

    @Autowired
    MemberRepository memberRepository;

    /** 카카오가 돌려준 사용자 정보로 가입·로그인하고 보던 화면으로 돌아간다 (AUTH-01). */
    @Test
    void kakao_success_creates_member_and_returns_to_page() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        KakaoLogin.remember(req, "/blog/abc/3");
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("id", 12345L, "kakao_account", Map.of("profile", Map.of("nickname", "카카오보리"))), "id");
        MockHttpServletResponse res = new MockHttpServletResponse();
        kakaoLogin.onAuthenticationSuccess(req, res, new OAuth2AuthenticationToken(user, user.getAuthorities(), "kakao"));

        assertThat(res.getRedirectedUrl()).isEqualTo("/blog/abc/3");
        var member = memberRepository.findBySocialProviderAndSocialId("KAKAO", "12345").orElseThrow();
        assertThat(member.getNickname()).isEqualTo("카카오보리");
        assertThat(member.getRole()).isEqualTo("MEMBER");
        assertThat(req.getSession().getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
    }

    /** 실패·취소하면 아무것도 만들지 않고 로그인 화면에 이유를 알린다. 다른 사이트 주소로는 안 보낸다. */
    @Test
    void kakao_failure_and_unsafe_redirect() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        KakaoLogin.remember(req, "//evil.example");
        MockHttpServletResponse res = new MockHttpServletResponse();
        long before = memberRepository.count();
        kakaoLogin.onAuthenticationFailure(req, res, new OAuth2AuthenticationException("access_denied"));
        assertThat(res.getRedirectedUrl()).isEqualTo("/login?error=kakao&redirect=%2F");
        assertThat(memberRepository.count()).isEqualTo(before);
    }

    @Test
    void providers_without_kakao_keys() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.kakao").value(false)).andExpect(jsonPath("$.dev").value(true));
    }
}
