package com.blogdock.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 블로그 API가 주고받는 모양들. */
public final class BlogDtos {

    private BlogDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "블로그 주소를 입력해 주세요") String address,
            @NotBlank(message = "블로그 이름을 입력해 주세요")
            @Size(max = 40, message = "블로그 이름은 40자 이하로 입력해 주세요") String name,
            @Size(max = 500, message = "소개는 500자 이하로 입력해 주세요") String description) {
    }

    /** profileImageUrl: 이미지 업로드로 받은 주소. 비우면 기본 이미지. */
    public record UpdateRequest(
            @NotBlank(message = "블로그 이름을 입력해 주세요")
            @Size(max = 40, message = "블로그 이름은 40자 이하로 입력해 주세요") String name,
            @Size(max = 500, message = "소개는 500자 이하로 입력해 주세요") String description,
            @Pattern(regexp = "^$|^/files/[A-Za-z0-9/_.-]+$", message = "프로필 이미지를 다시 올려 주세요")
            String profileImageUrl) {
    }

    /** reason: FORMAT(규칙 위반) / RESERVED(예약어) / TAKEN(이미 있음) / null(사용 가능) */
    public record AddressCheckResponse(boolean available, String reason) {
    }

    public record BlogResponse(String address, String name, String description,
                               String profileImageUrl, boolean isOwner) {

        public static BlogResponse of(Blog blog, Long viewerId) {
            return new BlogResponse(blog.getAddress(), blog.getName(), blog.getDescription(),
                    blog.getProfileImageUrl(), viewerId != null && blog.isOwnedBy(viewerId));
        }
    }
}
