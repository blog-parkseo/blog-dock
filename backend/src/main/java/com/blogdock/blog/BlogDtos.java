package com.blogdock.blog;

import com.blogdock.subscription.SubscriptionService.SubscriptionResponse;

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

    /**
     * profileImageUrl: 이미지 업로드로 받은 주소. 비우면 기본 이미지.
     * cover: 배경 코드, AUTO면 자동 색으로 되돌린다, 안 보내면(null) 그대로 둔다.
     */
    public record UpdateRequest(
            @NotBlank(message = "블로그 이름을 입력해 주세요")
            @Size(max = 40, message = "블로그 이름은 40자 이하로 입력해 주세요") String name,
            @Size(max = 500, message = "소개는 500자 이하로 입력해 주세요") String description,
            @Pattern(regexp = "^$|^/files/[A-Za-z0-9/_.-]+$", message = "프로필 이미지를 다시 올려 주세요")
            String profileImageUrl,
            @Pattern(regexp = "^(AUTO|SUNSET|MEADOW|SKYFIELD|NIGHT|SKY|MINT|LAVENDER|LEMON|ROSE|GRAY)$",
                    message = "배경을 다시 골라 주세요") String cover) {
    }

    /** reason: FORMAT(규칙 위반) / RESERVED(예약어) / TAKEN(이미 있음) / null(사용 가능) */
    public record AddressCheckResponse(boolean available, String reason) {
    }

    /**
     * subscriberCount: 구독자 수, subscribed: 보는 사람이 구독했는지 (SUB-01),
     * subscribesMe: 블로그 주인이 보는 사람의 블로그를 구독했는지 (SUB-05, 둘 다 참이면 맞구독).
     */
    public record BlogResponse(String address, String name, String description,
                               String profileImageUrl, String cover, boolean isOwner, long subscriberCount, boolean subscribed,
                               boolean subscribesMe) {

        public static BlogResponse of(Blog blog, Long viewerId, SubscriptionResponse subscription) {
            return new BlogResponse(blog.getAddress(), blog.getName(), blog.getDescription(),
                    blog.getProfileImageUrl(), blog.getCover(), viewerId != null && blog.isOwnedBy(viewerId),
                    subscription.subscriberCount(), subscription.subscribed(), subscription.subscribesMe());
        }
    }
}
