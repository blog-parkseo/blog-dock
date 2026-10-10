package com.blogdock.image;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blogdock.auth.LoginMember;

@RestController
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/api/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse upload(@AuthenticationPrincipal LoginMember login,
                                @RequestParam(value = "file", required = false) MultipartFile file) {
        Image image = imageService.upload(LoginMember.require(login), file);
        return new ImageResponse(image.getId(), image.url(), image.thumbUrl());
    }

    public record ImageResponse(Long id, String url, String thumbUrl) {
    }
}
