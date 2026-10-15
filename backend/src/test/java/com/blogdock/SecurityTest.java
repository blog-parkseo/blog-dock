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

import com.blogdock.auth.SocialLogin;
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
    SocialLogin socialLogin;

    @Autowired
    MemberRepository memberRepository;

    /** 카카오가 돌려준 사용자 정보로 가입·로그인하고 보던 화면으로 돌아간다 (AUTH-01). */
    @Test
    void kakao_success_creates_member_and_returns_to_page() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        SocialLogin.remember(req, "/blog/abc/3");
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("id", 12345L, "kakao_account", Map.of("profile", Map.of("nickname", "카카오보리"))), "id");
        MockHttpServletResponse res = new MockHttpServletResponse();
        socialLogin.onAuthenticationSuccess(req, res, new OAuth2AuthenticationToken(user, user.getAuthorities(), "kakao"));

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
        SocialLogin.remember(req, "//evil.example");
        MockHttpServletResponse res = new MockHttpServletResponse();
        long before = memberRepository.count();
        socialLogin.onAuthenticationFailure(req, res, new OAuth2AuthenticationException("access_denied"));
        assertThat(res.getRedirectedUrl()).isEqualTo("/login?error=kakao&reason=access_denied&redirect=%2F");
        assertThat(memberRepository.count()).isEqualTo(before);
    }

    /** 구글이 돌려준 sub·name·picture로 가입한다. 카카오 회원과 번호가 같아도 다른 회원이다. */
    @Test
    void google_success_creates_member() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        SocialLogin.remember(req, "/");
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("sub", "12345", "name", "구글보리", "picture", "https://example.com/p.png"), "sub");
        MockHttpServletResponse res = new MockHttpServletResponse();
        socialLogin.onAuthenticationSuccess(req, res, new OAuth2AuthenticationToken(user, user.getAuthorities(), "google"));

        assertThat(res.getRedirectedUrl()).isEqualTo("/");
        var member = memberRepository.findBySocialProviderAndSocialId("GOOGLE", "12345").orElseThrow();
        assertThat(member.getNickname()).isEqualTo("구글보리");
        assertThat(member.getProfileImageUrl()).isEqualTo("https://example.com/p.png");
    }

    @Test
    void google_failure_returns_google_error() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        SocialLogin.remember(req, "/blog/abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        socialLogin.onAuthenticationFailure(req, res, new OAuth2AuthenticationException("access_denied"));
        assertThat(res.getRedirectedUrl()).isEqualTo("/login?error=google&reason=access_denied&redirect=%2Fblog%2Fabc");
    }

    /** 깃허브는 이름이 없으면 login 아이디를 닉네임으로 쓴다. */
    @Test
    void github_success_uses_login_when_no_name() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        SocialLogin.remember(req, "/");
        var attrs = new java.util.HashMap<String, Object>();
        attrs.put("id", 777L);
        attrs.put("login", "bori-dev");
        attrs.put("name", null);
        attrs.put("avatar_url", "https://example.com/a.png");
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attrs, "id");
        MockHttpServletResponse res = new MockHttpServletResponse();
        socialLogin.onAuthenticationSuccess(req, res, new OAuth2AuthenticationToken(user, user.getAuthorities(), "github"));

        var member = memberRepository.findBySocialProviderAndSocialId("GITHUB", "777").orElseThrow();
        assertThat(member.getNickname()).isEqualTo("bori-dev");
        assertThat(member.getProfileImageUrl()).isEqualTo("https://example.com/a.png");
    }

    @Test
    void providers_without_social_keys() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.kakao").value(false))
                .andExpect(jsonPath("$.google").value(false)).andExpect(jsonPath("$.github").value(false)).andExpect(jsonPath("$.dev").value(true));
    }
}
