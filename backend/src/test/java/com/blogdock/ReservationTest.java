package com.blogdock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** SUB-07 예약 맞구독: 블로그가 없는 사람에게 맞구독을 눌러 두면 블로그를 만들 때 자동으로 구독된다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reserve;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class ReservationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    MockHttpSession login(String nickname) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/dev-login").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}")).andExpect(status().isOk());
        return session;
    }

    ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
        return mvc.perform(req.session(s).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    JsonNode notifications(MockHttpSession s) throws Exception {
        return json.readTree(mvc.perform(get("/api/notifications").session(s)).andReturn().getResponse()
                .getContentAsString()).get("items");
    }

    @Test
    void reserve_then_blog_created() throws Exception {
        MockHttpSession seohyun = login("예약서현");
        send(post("/api/blogs"), seohyun, Map.of("address", "rsv-seohyun", "name", "서현의 블로그")).andExpect(status().isCreated());
        MockHttpSession hyunseo = login("hyunseo");
        mvc.perform(post("/api/blogs/rsv-seohyun/subscription").session(hyunseo)).andExpect(status().isOk());

        long id = notifications(seohyun).get(0).get("id").asLong();
        String path = "/api/notifications/" + id + "/subscribe-back";

        // 블로그가 없으면 예약, 취소, 다시 예약
        mvc.perform(post(path).session(seohyun)).andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(true)).andExpect(jsonPath("$.read").value(true));
        mvc.perform(delete(path).session(seohyun)).andExpect(jsonPath("$.reserved").value(false));
        mvc.perform(post(path).session(seohyun)).andExpect(jsonPath("$.reserved").value(true));
        // 남의 알림으로는 못 한다
        mvc.perform(post(path).session(hyunseo)).andExpect(status().isNotFound());

        // hyunseo가 블로그를 만들면 서현이 자동으로 구독, 맞구독이 된다
        send(post("/api/blogs"), hyunseo, Map.of("address", "rsv-hyunseo", "name", "현서의 블로그"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/blogs/rsv-hyunseo").session(seohyun))
                .andExpect(jsonPath("$.subscribed").value(true)).andExpect(jsonPath("$.subscribesMe").value(true));
        mvc.perform(get("/api/blogs/rsv-hyunseo")).andExpect(jsonPath("$.subscriberCount").value(1));

        // 서현에게: 맞구독 됨 알림, 구독 알림은 이제 맞구독중
        JsonNode mine = notifications(seohyun);
        org.junit.jupiter.api.Assertions.assertEquals("hyunseo님이 블로그를 만들어서 맞구독이 됐어요",
                mine.get(0).get("message").asText());
        org.junit.jupiter.api.Assertions.assertEquals("rsv-hyunseo", mine.get(0).get("address").asText());
        org.junit.jupiter.api.Assertions.assertTrue(mine.get(1).get("subscribed").asBoolean());
        org.junit.jupiter.api.Assertions.assertFalse(mine.get(1).get("reserved").asBoolean());
        // hyunseo에게: 서현이 구독했다는 알림
        org.junit.jupiter.api.Assertions.assertEquals("예약서현님이 내 블로그를 구독했어요",
                notifications(hyunseo).get(0).get("message").asText());

        // 블로그가 있는 사람에게 누르면 바로 구독 (예약 아님)
        MockHttpSession third = login("세번째");
        send(post("/api/blogs"), third, Map.of("address", "rsv-third", "name", "셋")).andExpect(status().isCreated());
        mvc.perform(post("/api/blogs/rsv-seohyun/subscription").session(third)).andExpect(status().isOk());
        long id2 = notifications(seohyun).get(0).get("id").asLong();
        mvc.perform(post("/api/notifications/" + id2 + "/subscribe-back").session(seohyun))
                .andExpect(jsonPath("$.subscribed").value(true)).andExpect(jsonPath("$.reserved").value(false));
    }
}
