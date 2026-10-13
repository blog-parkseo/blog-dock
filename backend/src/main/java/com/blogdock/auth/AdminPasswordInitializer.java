package com.blogdock.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 관리자 계정(아이디 admin)의 비밀번호를 서버가 켜질 때 넣는다 (ADMIN-01).
 * 비밀번호를 코드나 DB 마이그레이션에 쓰지 않으려고, 운영에서는 ADMIN_PASSWORD(GitHub Secret)로 받는다.
 * 값이 없으면 관리자는 비밀번호로 로그인할 수 없다.
 */
@Component
public class AdminPasswordInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public AdminPasswordInitializer(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                                    @Value("${blogdock.admin-password:}") String adminPassword) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminPassword)) {
            return;
        }
        memberRepository.findByLoginId("admin").ifPresent(admin -> {
            if (admin.getPasswordHash() == null || !passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
                admin.changePasswordHash(passwordEncoder.encode(adminPassword));
            }
        });
    }
}
