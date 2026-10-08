package com.blogdock.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 방어는 4단계(보안)에서 켠다
                .csrf(csrf -> csrf.disable())
                // H2 콘솔 화면이 iframe을 쓰기 때문에 같은 출처는 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")                 // ADMIN-01
                        .requestMatchers("/api/health", "/api/auth/dev-login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/**").permitAll()      // 블로그 보기는 누구나
                        .requestMatchers("/api/**").authenticated()                        // 나머지 API는 로그인 필요
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, 401, "LOGIN_REQUIRED", "로그인이 필요해요"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, 403, "FORBIDDEN", "권한이 없어요")))
                .logout(logout -> logout                                                   // AUTH-02
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
                        .deleteCookies("JSESSIONID"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    private void writeError(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(code, message));
    }
}
