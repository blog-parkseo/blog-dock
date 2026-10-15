package com.blogdock.tag;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;

@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/blogs/{address}/tags")
    public List<TagService.TagCount> list(@PathVariable String address, @AuthenticationPrincipal LoginMember login) {
        return tagService.counts(address, LoginMember.idOf(login));
    }
}
