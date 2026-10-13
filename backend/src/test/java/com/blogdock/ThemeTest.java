package com.blogdock;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** HOME-05 테마: 글·카테고리 기본 테마, 홈 테마 목록과 테마 피드. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:theme;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class ThemeTest {

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

    Map<String, Object> body(String title, String visibility, String theme) {
        Map<String, Object> b = new HashMap<>();
        b.put("title", title);
        b.put("contentMarkdown", title);
        b.put("contentHtml", "<p>" + title + " 본문</p>");
        b.put("publish", true);
        b.put("visibility", visibility);
        b.put("theme", theme);
        return b;
    }

    JsonNode write(MockHttpSession s, Map<String, Object> body) throws Exception {
        return json.readTree(send(post("/api/posts"), s, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void theme_feed() throws Exception {
        MockHttpSession owner = login("테마주인");
        send(post("/api/blogs"), owner, Map.of("address", "theme-blog", "name", "테마 블로그")).andExpect(status().isCreated());
        MockHttpSession reader = login("테마독자");

        // 카테고리 기본 테마
        JsonNode cat = json.readTree(send(post("/api/blogs/theme-blog/categories"), owner, Map.of("name", "제주도"))
                .andReturn().getResponse().getContentAsString());
        send(patch("/api/categories/" + cat.get("id").asLong() + "/theme"), owner, Map.of("theme", "TRAVEL"))
                .andExpect(jsonPath("$.theme").value("TRAVEL"));
        mvc.perform(get("/api/blogs/theme-blog/categories")).andExpect(jsonPath("$[0].theme").value("TRAVEL"));
        send(patch("/api/categories/" + cat.get("id").asLong() + "/theme"), owner, Map.of("theme", "SPACE"))
                .andExpect(status().isBadRequest());

        JsonNode jeju = write(owner, body("제주 한 바퀴", "PUBLIC", "TRAVEL"));
        write(owner, body("부산 바다", "PUBLIC", "TRAVEL"));
        write(owner, body("구독자 여행기", "SUBSCRIBERS", "TRAVEL"));
        write(owner, body("비밀 여행", "PRIVATE", "TRAVEL"));
        write(owner, body("라면 맛집", "PUBLIC", "FOOD"));
        write(owner, body("테마 없는 글", "PUBLIC", null));
        send(post("/api/posts"), owner, body("이상한 테마", "PUBLIC", "SPACE")).andExpect(status().isBadRequest());

        // 공감·댓글 수
        mvc.perform(post("/api/posts/" + jeju.get("id").asLong() + "/like").session(reader)).andExpect(status().isOk());
        send(post("/api/posts/" + jeju.get("id").asLong() + "/comments"), reader, Map.of("content", "가고 싶다"))
                .andExpect(status().isCreated());

        // 테마 목록: 공개 글만 센다
        mvc.perform(get("/api/home/themes"))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].code").value("DAILY")).andExpect(jsonPath("$[0].count").value(0))
                .andExpect(jsonPath("$[1].code").value("TRAVEL")).andExpect(jsonPath("$[1].count").value(2))
                .andExpect(jsonPath("$[1].label").value("여행"))
                .andExpect(jsonPath("$[2].count").value(1));

        // 테마 피드: 최신순, 공감·댓글 수와 블로그 정보
        mvc.perform(get("/api/home/themes/TRAVEL/posts"))
                .andExpect(jsonPath("$.theme.title").value("떠나고 싶은 날"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("부산 바다"))
                .andExpect(jsonPath("$.items[1].title").value("제주 한 바퀴"))
                .andExpect(jsonPath("$.items[1].likeCount").value(1))
                .andExpect(jsonPath("$.items[1].commentCount").value(1))
                .andExpect(jsonPath("$.items[1].blog.name").value("테마 블로그"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
        mvc.perform(get("/api/home/themes/SPACE/posts")).andExpect(status().isNotFound());

        // 5개씩 더보기
        for (int i = 0; i < 5; i++) {
            write(owner, body("일상 " + i, "PUBLIC", "DAILY"));
        }
        write(owner, body("일상 5", "PUBLIC", "DAILY"));
        String cursor = json.readTree(mvc.perform(get("/api/home/themes/DAILY/posts"))
                .andExpect(jsonPath("$.items", hasSize(5))).andReturn().getResponse().getContentAsString())
                .get("nextCursor").asText();
        mvc.perform(get("/api/home/themes/DAILY/posts").param("cursor", cursor))
                .andExpect(jsonPath("$.items", hasSize(1))).andExpect(jsonPath("$.items[0].title").value("일상 0"));

        // 글 화면과 수정 화면에 테마, 수정하면 테마가 바뀐다
        String slug = jeju.get("slug").asText();
        mvc.perform(get("/api/blogs/theme-blog/posts/" + slug)).andExpect(jsonPath("$.theme.label").value("여행"));
        mvc.perform(get("/api/posts/" + jeju.get("id").asLong() + "/edit").session(owner))
                .andExpect(jsonPath("$.theme").value("TRAVEL"));
        send(put("/api/posts/" + jeju.get("id").asLong()), owner, body("제주 한 바퀴", "PUBLIC", ""))
                .andExpect(status().isOk());
        mvc.perform(get("/api/blogs/theme-blog/posts/" + slug)).andExpect(jsonPath("$.theme").doesNotExist());
        mvc.perform(get("/api/home/themes/TRAVEL/posts")).andExpect(jsonPath("$.items", hasSize(1)));
    }
}
