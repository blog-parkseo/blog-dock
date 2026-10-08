package com.blogdock.post;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Size;

public final class PostDtos {

    private PostDtos() {
    }

    /**
     * 글 저장. publish=true면 발행, false면 임시저장.
     * thumbnailUrl: 대표로 고른 본문 이미지 주소(/files/...). 비우면 본문 첫 이미지.
     * 제목·본문 검사는 서비스에서 "제목 → 본문" 순서로 첫 문제만 알린다 (POST-01).
     */
    public record WriteRequest(
            String title,
            @Size(max = 2_000_000, message = "본문이 너무 길어요") String contentMarkdown,
            @Size(max = 4_000_000, message = "본문이 너무 길어요") String contentHtml,
            Long categoryId,
            List<String> tags,
            String visibility,
            boolean publish,
            String thumbnailUrl) {
    }

    public record VisibilityRequest(String visibility) {
    }

    public record SavedResponse(Long id, String slug, String status, String blogAddress) {
    }

    public record CategoryRef(Long id, String name) {
    }

    public record BlogRef(String address, String name) {
    }

    public record Neighbor(String slug, String title) {
    }

    /** 목록 한 줄 (BLOG-03, CAT-02, TAG-02, SRCH-01, HOME-01, MNG-01). */
    public record PostSummary(Long id, String slug, String title, String excerpt, Instant publishedAt,
                              Instant updatedAt, CategoryRef category, String thumbnailUrl, String visibility,
                              String status, BlogRef blog) {
    }

    public record PageResponse<T>(List<T> items, int page, int totalPages, long totalElements, boolean hasNext) {
    }

    public record HomeResponse(List<PostSummary> items, String nextCursor) {
    }

    /** POST-04 글 상세. */
    public record PostDetail(Long id, String slug, String title, String contentHtml, String visibility,
                             Instant publishedAt, Instant updatedAt, CategoryRef category, List<String> tags,
                             String thumbnailUrl, long likeCount, boolean liked, long commentCount,
                             boolean isOwner, Neighbor prev, Neighbor next, BlogRef blog) {
    }

    /** 수정 화면에 채울 값 (주인만). */
    public record PostEdit(Long id, String slug, String title, String contentMarkdown, String contentHtml,
                           Long categoryId, List<String> tags, String visibility, String status,
                           String thumbnailUrl, String blogAddress) {
    }
}
