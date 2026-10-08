package com.blogdock.category;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record NameRequest(
            @NotBlank(message = "카테고리 이름을 입력해 주세요")
            @Size(max = 20, message = "카테고리 이름은 20자 이하로 입력해 주세요") String name) {
    }

    public record OrderRequest(@NotNull(message = "순서를 보내 주세요") List<Long> ids) {
    }

    public record CategoryResponse(Long id, String name, int sortOrder) {

        public static CategoryResponse of(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSortOrder());
        }
    }
}
