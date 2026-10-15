package com.blogdock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.blogdock.auth.MemberRepository;

/** AUTH-01 아이디·비밀번호 가입·로그인. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pwlogin;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class PasswordLoginTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MemberRepository members;

    ResultActions signup(String body, MockHttpSession session) throws Exception {
        return mvc.perform(post("/api/auth/signup").session(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    ResultActions login(String loginId, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void signup_logs_in_and_stores_only_a_hash() throws Exception {
        MockHttpSession session = new MockHttpSession();
        signup("{\"loginId\":\"seohyun\",\"password\":\"blog2026ok\",\"nickname\":\"서현\"}", session)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("서현"));
        mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());

        String hash = members.findByLoginId("seohyun").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2a$").doesNotContain("blog2026ok");

        login("seohyun", "blog2026ok").andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("서현"));
        signup("{\"loginId\":\"seohyun\",\"password\":\"other1234\",\"nickname\":\"다른 사람\"}", new MockHttpSession())
                .andExpect(status().isConflict());
    }

    @Test
    void signup_rules() throws Exception {
        signup("{\"loginId\":\"1abc\",\"password\":\"blog2026ok\",\"nickname\":\"a\"}", new MockHttpSession())
                .andExpect(status().isBadRequest());
        signup("{\"loginId\":\"goodid\",\"password\":\"onlyletters\",\"nickname\":\"a\"}", new MockHttpSession())
                .andExpect(status().isBadRequest());
        signup("{\"loginId\":\"goodid\",\"password\":\"short1\",\"nickname\":\"a\"}", new MockHttpSession())
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrong_password_and_unknown_id_look_the_same() throws Exception {
        signup("{\"loginId\":\"samemsg\",\"password\":\"right1234\",\"nickname\":\"a\"}", new MockHttpSession());
        login("samemsg", "wrong1234").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 맞지 않아요"));
        login("nobody", "wrong1234").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 맞지 않아요"));
    }

    @Test
    void five_failures_block_the_id() throws Exception {
        signup("{\"loginId\":\"locked\",\"password\":\"right1234\",\"nickname\":\"a\"}", new MockHttpSession());
        for (int i = 0; i < 5; i++) {
            login("locked", "wrong1234").andExpect(status().isUnauthorized());
        }
        login("locked", "right1234").andExpect(status().isTooManyRequests());
    }

    @Test
    void admin_logs_in_with_configured_password() throws Exception {
        login("admin", "admin1234").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
