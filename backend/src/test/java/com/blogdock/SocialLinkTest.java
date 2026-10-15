package com.blogdock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.blogdock.auth.AuthService;
import com.blogdock.auth.AuthService.LinkResult;
import com.blogdock.auth.LoginMember;
import com.blogdock.auth.Member;
import com.blogdock.auth.SocialLogin;
import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogRepository;
import com.blogdock.common.ApiException;

/** 계정 연결: 카카오로 만든 회원에 구글·깃허브를 붙이면 어느 걸로 들어와도 같은 회원이다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:link;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class SocialLinkTest {

    @Autowired
    AuthService authService;

    @Autowired
    SocialLogin socialLogin;

    @Autowired
    BlogRepository blogRepository;

    @Test
    void linked_google_logs_into_same_member_and_can_be_unlinked() {
        Member me = authService.socialLogin("KAKAO", "k-1", "서현", null);
        assertThat(authService.linkSocial(me.getId(), "GOOGLE", "g-1")).isEqualTo(LinkResult.LINKED);

        assertThat(authService.socialLogin("GOOGLE", "g-1", "구글이름", null).getId()).isEqualTo(me.getId());
        assertThat(authService.socials(me.getId())).extracting(AuthService.SocialAccount::provider)
                .containsExactly("kakao", "google");

        // 해제하면 구글로 들어올 때 새 회원이 된다. 마지막 남은 카카오는 해제할 수 없다
        authService.unlinkSocial(me.getId(), "google");
        assertThat(authService.socialLogin("GOOGLE", "g-1", "구글이름", null).getId()).isNotEqualTo(me.getId());
        assertThatThrownBy(() -> authService.unlinkSocial(me.getId(), "kakao")).isInstanceOf(ApiException.class);
        // 카카오도 해제 안 됐으니 카카오로는 그대로 같은 회원
        assertThat(authService.socialLogin("KAKAO", "k-1", "서현", null).getId()).isEqualTo(me.getId());
    }

    @Test
    void empty_account_moves_but_account_with_blog_is_not_taken() {
        Member me = authService.socialLogin("KAKAO", "k-2", "나", null);
        // 실수로 구글로 먼저 들어와 생긴 빈 회원 → 연결하면 나에게 옮겨 온다
        authService.socialLogin("GOOGLE", "g-2", "빈 회원", null);
        assertThat(authService.linkSocial(me.getId(), "GOOGLE", "g-2")).isEqualTo(LinkResult.LINKED);
        assertThat(authService.socialLogin("GOOGLE", "g-2", "x", null).getId()).isEqualTo(me.getId());

        // 블로그가 있는 다른 사람의 깃허브는 가져오지 않는다
        Member other = authService.socialLogin("GITHUB", "h-2", "남", null);
        blogRepository.save(Blog.open(other.getId(), "other-blog-2", "남의 블로그", null));
        assertThat(authService.linkSocial(me.getId(), "GITHUB", "h-2")).isEqualTo(LinkResult.USED_BY_OTHER);
        assertThat(authService.socialLogin("GITHUB", "h-2", "남", null).getId()).isEqualTo(other.getId());

        // 이미 구글이 연결돼 있으면 다른 구글 계정은 못 붙인다
        assertThat(authService.linkSocial(me.getId(), "GOOGLE", "g-3")).isEqualTo(LinkResult.ALREADY_HAS_PROVIDER);
    }

    /** 연결하러 갔다 돌아오면 소셜 회원으로 바뀌지 않고, 연결하던 회원으로 로그인이 유지된다. */
    @Test
    void link_mode_keeps_the_linking_member_logged_in() throws Exception {
        Member me = authService.socialLogin("KAKAO", "k-4", "유지", null);
        MockHttpServletRequest req = new MockHttpServletRequest();
        SocialLogin.rememberLink(req, me.getId());
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("id", 4444L, "login", "linker"), "id");
        MockHttpServletResponse res = new MockHttpServletResponse();
        socialLogin.onAuthenticationSuccess(req, res, new OAuth2AuthenticationToken(user, user.getAuthorities(), "github"));

        assertThat(res.getRedirectedUrl()).isEqualTo("/settings?linked=github");
        var context = (SecurityContext) req.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(((LoginMember) context.getAuthentication().getPrincipal()).id()).isEqualTo(me.getId());
        assertThat(authService.socialLogin("GITHUB", "4444", "linker", null).getId()).isEqualTo(me.getId());
    }
}
