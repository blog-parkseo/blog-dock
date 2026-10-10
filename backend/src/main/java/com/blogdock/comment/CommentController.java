package com.blogdock.comment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.comment.CommentService.CommentResponse;
import com.blogdock.common.Idempotency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class CommentController {

    private final CommentService commentService;
    private final Idempotency idempotency;

    public CommentController(CommentService commentService, Idempotency idempotency) {
        this.commentService = commentService;
        this.idempotency = idempotency;
    }

    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> list(@PathVariable Long postId, @AuthenticationPrincipal LoginMember login) {
        return commentService.list(postId, LoginMember.idOf(login));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<?> write(@PathVariable Long postId, @AuthenticationPrincipal LoginMember login,
                                   @RequestHeader(value = Idempotency.HEADER, required = false) String key,
                                   @Valid @RequestBody ContentRequest req) {
        Long memberId = LoginMember.require(login);
        return idempotency.run(memberId, key, java.util.Map.of("postId", postId, "content", req.content()),
                HttpStatus.CREATED, () -> commentService.write(postId, memberId, req.content()));
    }

    @PatchMapping("/api/comments/{id}")
    public CommentResponse edit(@PathVariable Long id, @AuthenticationPrincipal LoginMember login,
                                @Valid @RequestBody ContentRequest req) {
        return commentService.edit(id, LoginMember.require(login), req.content());
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        commentService.delete(id, LoginMember.require(login));
    }

    public record ContentRequest(
            @NotBlank(message = "댓글을 입력해 주세요")
            @Size(max = 1000, message = "댓글은 1000자 이하로 입력해 주세요") String content) {
    }
}
