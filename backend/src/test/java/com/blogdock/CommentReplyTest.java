package com.blogdock;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/** 답글: 누구나 달고, 한 단계만, 비밀 댓글의 답글은 비밀, 원래 댓글을 지우면 같이 지워지고, 알림이 간다. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reply;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(CsrfForTests.class)
class CommentReplyTest {

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

    long id(ResultActions r) throws Exception {
        return json.readTree(r.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();
    }

    long publish(MockHttpSession s, String title) throws Exception {
        return id(send(post("/api/posts"), s, Map.of("title", title, "contentMarkdown", title,
                "contentHtml", "<p>" + title + "</p>", "publish", true, "visibility", "PUBLIC")));
    }

    @Test
    void replies() throws Exception {
        MockHttpSession owner = login("rp-owner");
        send(post("/api/blogs"), owner, Map.of("address", "rp-blog", "name", "답글 블로그")).andExpect(status().isCreated());
        long postId = publish(owner, "답글 받을 글");
        String url = "/api/posts/" + postId + "/comments";
        MockHttpSession a = login("rp-a");
        MockHttpSession b = login("rp-b");

        long first = id(send(post(url), a, Map.of("content", "첫 댓글")));
        mvc.perform(post("/api/comments/" + first + "/replies")).andExpect(status().isUnauthorized());
        long ownerReply = id(send(post("/api/comments/" + first + "/replies"), owner, Map.of("content", "고마워요")));
        // 답글에 단 답글은 원래 댓글 밑으로
        long bReply = id(send(post("/api/comments/" + ownerReply + "/replies"), b, Map.of("content", "저도요")));

        mvc.perform(get(url))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].byPostOwner").value(false))
                .andExpect(jsonPath("$[1].id").value(ownerReply))
                .andExpect(jsonPath("$[1].parentId").value(first))
                .andExpect(jsonPath("$[1].byPostOwner").value(true))
                .andExpect(jsonPath("$[2].id").value(bReply))
                .andExpect(jsonPath("$[2].parentId").value(first));

        // 알림: a는 답글 2개, 글 주인은 a의 댓글 + b의 답글 (자기 답글은 빼고)
        mvc.perform(get("/api/notifications").session(a))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].message").value("rp-b님이 “답글 받을 글”의 내 댓글에 답글을 남겼어요"))
                .andExpect(jsonPath("$.items[0].slug").exists());
        mvc.perform(get("/api/notifications").session(owner)).andExpect(jsonPath("$.items", hasSize(2)));

        // 비밀 댓글: 남은 답글을 못 달고, 글 주인의 답글은 늘 비밀이라 댓글 쓴 사람과 글 주인만 본다
        long secret = id(send(post(url), a, Map.of("content", "몰래", "secret", true)));
        send(post("/api/comments/" + secret + "/replies"), b, Map.of("content", "궁금"))
                .andExpect(status().isForbidden());
        long secretReply = id(send(post("/api/comments/" + secret + "/replies"), owner, Map.of("content", "몰래 답장")));
        mvc.perform(get(url).session(a)).andExpect(jsonPath("$[4].content").value("몰래 답장"))
                .andExpect(jsonPath("$[4].secret").value(true));
        mvc.perform(get(url).session(b)).andExpect(jsonPath("$[4].content").doesNotExist())
                .andExpect(jsonPath("$[4].parentId").value(secret));
        // 수정할 때 공개로 바꿔도 비밀 그대로
        send(patch("/api/comments/" + secretReply), owner, Map.of("content", "고침", "secret", false))
                .andExpect(jsonPath("$.secret").value(true));

        // 원래 댓글을 지우면 답글도 사라진다
        mvc.perform(delete("/api/comments/" + first).session(a)).andExpect(status().isNoContent());
        mvc.perform(get(url).session(owner)).andExpect(jsonPath("$", hasSize(2)));
    }
}
