package com.blogdock.config;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import com.blogdock.auth.KakaoLogin;
import com.blogdock.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 로그인 정보를 세션에 저장하는 곳 (AUTH-01, AUTH-03). */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ObjectProvider<ClientRegistrationRepository> kakaoClients,
                                           KakaoLogin kakaoLogin,
                                           @Value("${blogdock.cookie-domain:}") String cookieDomain) throws Exception {
        // CSRF: 서버가 XSRF-TOKEN 쿠키를 주고, 화면은 상태를 바꾸는 요청마다 X-XSRF-TOKEN 헤더로 돌려보낸다.
        // 다른 사이트는 이 쿠키를 읽을 수 없어서 요청을 흉내 낼 수 없다.
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(c -> {
            c.path("/");
            if (StringUtils.hasText(cookieDomain)) {
                c.domain(cookieDomain); // 블로그 서브도메인끼리 같이 쓴다
            }
        });
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null); // 매 요청마다 토큰을 읽어서 쿠키가 항상 있게 한다

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        .ignoringRequestMatchers("/h2-console/**"))
                // H2 콘솔 화면이 iframe을 쓰기 때문에 같은 출처는 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")                 // ADMIN-01
                        .requestMatchers("/api/health", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/**", "/api/home/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/comments").permitAll()
                        .requestMatchers("/api/**").authenticated()                        // 나머지 API는 로그인 필요
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, 401, "LOGIN_REQUIRED", "로그인이 필요해요"))
                        .accessDeniedHandler((req, res, e) -> {
                            if (e instanceof org.springframework.security.web.csrf.CsrfException) {
                                writeError(res, 403, "CSRF", "화면을 새로고침한 뒤 다시 시도해 주세요");
                            } else {
                                writeError(res, 403, "FORBIDDEN", "권한이 없어요");
                            }
                        }))
                .logout(logout -> logout                                                   // AUTH-02
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
                        .deleteCookies("JSESSIONID"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // 카카오 키를 넣고 kakao 프로필로 켰을 때만 카카오 로그인을 연다 (AUTH-01)
        if (kakaoClients.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(kakaoLogin)
                    .failureHandler(kakaoLogin));
        }
        return http.build();
    }

    private void writeError(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(code, message));
    }
}
