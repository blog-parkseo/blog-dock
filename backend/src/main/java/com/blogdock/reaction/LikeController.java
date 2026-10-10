package com.blogdock.reaction;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.reaction.LikeService.LikeResponse;

@RestController
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/api/posts/{postId}/like")
    public LikeResponse like(@PathVariable Long postId, @AuthenticationPrincipal LoginMember login) {
        return likeService.like(postId, LoginMember.require(login));
    }

    @DeleteMapping("/api/posts/{postId}/like")
    public LikeResponse unlike(@PathVariable Long postId, @AuthenticationPrincipal LoginMember login) {
        return likeService.unlike(postId, LoginMember.require(login));
    }
}
