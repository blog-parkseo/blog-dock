package com.blogdock;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** 테스트 요청마다 CSRF 토큰을 붙인다. CSRF 자체는 SecurityTest에서 따로 확인한다. */
@TestConfiguration
public class CsrfForTests {

    @Bean
    MockMvcBuilderCustomizer csrfByDefault() {
        return builder -> builder.defaultRequest(get("/").with(csrf()));
    }
}
