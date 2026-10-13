package com.blogdock;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
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

import com.blogdock.notification.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** NOTI-01 알림 만들기, NOTI-02 목록·읽음. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:noti;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class NotificationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    NotificationService notificationService;

    MockHttpSession login(String nickname) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/dev-login").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}")).andExpect(status().isOk());
        return session;
    }

    MockHttpSession blogOwner(String nickname, String address, String name) throws Exception {
        MockHttpSession s = login(nickname);
        send(post("/api/blogs"), s, Map.of("address", address, "name", name)).andExpect(status().isCreated());
        return s;
    }

    ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
        return mvc.perform(req.session(s).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    JsonNode write(MockHttpSession s, String title, String visibility, boolean publish) throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("title", title);
        b.put("contentMarkdown", title);
        b.put("contentHtml", "<p>" + title + "</p>");
        b.put("publish", publish);
        b.put("visibility", visibility);
        return json.readTree(send(post("/api/posts"), s, b).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    JsonNode list(MockHttpSession s) throws Exception {
        return json.readTree(mvc.perform(get("/api/notifications").session(s)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void comment_like_subscribe_new_post() throws Exception {
        MockHttpSession seohyun = blogOwner("서현", "noti-seohyun", "서현의 블로그");
        MockHttpSession minji = login("민지");
        JsonNode walk = write(seohyun, "한강 산책", "PUBLIC", true);
        long walkId = walk.get("id").asLong();

        // 구독 → 블로그 주인에게
        mvc.perform(post("/api/blogs/noti-seohyun/subscription").session(minji)).andExpect(status().isOk());
        // 댓글, 공감 → 글쓴이에게. 공감을 취소했다 다시 눌러도 한 번만
        send(post("/api/posts/" + walkId + "/comments"), minji, Map.of("content", "좋아요")).andExpect(status().isCreated());
        mvc.perform(post("/api/posts/" + walkId + "/like").session(minji)).andExpect(status().isOk());
        mvc.perform(delete("/api/posts/" + walkId + "/like").session(minji)).andExpect(status().isOk());
        mvc.perform(post("/api/posts/" + walkId + "/like").session(minji)).andExpect(status().isOk());
        // 내 글에 내가 단 댓글은 알리지 않는다
        send(post("/api/posts/" + walkId + "/comments"), seohyun, Map.of("content", "고마워요")).andExpect(status().isCreated());

        mvc.perform(get("/api/notifications/unread-count").session(seohyun)).andExpect(jsonPath("$.count").value(3));
        mvc.perform(get("/api/notifications").session(seohyun))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].message").value("민지님이 “한강 산책”에 공감했어요"))
                .andExpect(jsonPath("$.items[0].address").value("noti-seohyun"))
                .andExpect(jsonPath("$.items[1].message").value("민지님이 “한강 산책”에 댓글을 남겼어요"))
                .andExpect(jsonPath("$.items[2].message").value("민지님이 내 블로그를 구독했어요"));

        // 새 글: 구독자에게. 임시저장 → 발행 때 한 번, 수정은 다시 보내지 않는다. 비공개 글은 안 보낸다
        JsonNode draft = write(seohyun, "가을 하늘", "PUBLIC", false);
        mvc.perform(get("/api/notifications/unread-count").session(minji)).andExpect(jsonPath("$.count").value(0));
        Map<String, Object> pub = new HashMap<>(Map.of("title", "가을 하늘", "contentMarkdown", "x",
                "contentHtml", "<p>x</p>", "visibility", "PUBLIC", "publish", true));
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/posts/" + draft.get("id").asLong()),
                seohyun, pub).andExpect(status().isOk());
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/posts/" + draft.get("id").asLong()),
                seohyun, pub).andExpect(status().isOk());
        write(seohyun, "나만 보는 글", "PRIVATE", true);
        write(seohyun, "구독자 글", "SUBSCRIBERS", true);
        JsonNode mine = list(minji);
        org.junit.jupiter.api.Assertions.assertEquals(2, mine.get("items").size());
        org.junit.jupiter.api.Assertions.assertEquals("서현의 블로그에 새 글 “구독자 글”이 올라왔어요",
                mine.get("items").get(0).get("message").asText());
        org.junit.jupiter.api.Assertions.assertEquals("서현의 블로그에 새 글 “가을 하늘”이 올라왔어요",
                mine.get("items").get(1).get("message").asText());

        // 누르면 읽음. 글이 지워졌으면 볼 수 없는 글
        long firstId = mine.get("items").get(1).get("id").asLong();
        mvc.perform(post("/api/notifications/" + firstId + "/read").session(minji))
                .andExpect(jsonPath("$.read").value(true)).andExpect(jsonPath("$.available").value(true));
        mvc.perform(get("/api/notifications/unread-count").session(minji)).andExpect(jsonPath("$.count").value(1));
        mvc.perform(delete("/api/posts/" + draft.get("id").asLong()).session(seohyun));
        mvc.perform(post("/api/notifications/" + firstId + "/read").session(minji))
                .andExpect(jsonPath("$.available").value(false));
        // 구독을 끊으면 구독자 공개 글은 볼 수 없는 글
        mvc.perform(delete("/api/blogs/noti-seohyun/subscription").session(minji));
        mvc.perform(get("/api/notifications").session(minji)).andExpect(jsonPath("$.items[0].available").value(false));

        // 남의 알림은 못 읽는다, 모두 읽음
        mvc.perform(post("/api/notifications/" + firstId + "/read").session(seohyun)).andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/read-all").session(seohyun)).andExpect(jsonPath("$.count").value(3));
        mvc.perform(get("/api/notifications/unread-count").session(seohyun)).andExpect(jsonPath("$.count").value(0));

        // 비회원은 401, 청소는 최근 알림을 지우지 않는다
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        notificationService.cleanup();
        mvc.perform(get("/api/notifications").session(seohyun)).andExpect(jsonPath("$.items", hasSize(3)));
    }
}
