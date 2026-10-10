package com.blogdock.category;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.category.CategoryDtos.CategoryResponse;
import com.blogdock.common.ApiException;

/** CAT-01 카테고리 추가·이름 변경·삭제, CAT-04 순서 변경. 주인만 한다. */
@Service
public class CategoryService {

    /** 화면이 따로 쓰는 이름이라 카테고리로 만들 수 없다. */
    private static final Set<String> FIXED_NAMES = Set.of("전체 글", "미분류");

    private final CategoryRepository categoryRepository;
    private final BlogAccess blogAccess;

    public CategoryService(CategoryRepository categoryRepository, BlogAccess blogAccess) {
        this.categoryRepository = categoryRepository;
        this.blogAccess = blogAccess;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(String address) {
        Blog blog = blogAccess.find(address);
        return categoryRepository.findByBlogIdOrderBySortOrderAscIdAsc(blog.getId()).stream()
                .map(CategoryResponse::of).toList();
    }

    @Transactional
    public CategoryResponse create(String address, Long memberId, String rawName) {
        Blog blog = blogAccess.findOwned(address, memberId);
        String name = checkName(blog.getId(), rawName);
        int order = categoryRepository.maxSortOrder(blog.getId()) + 1;
        return CategoryResponse.of(categoryRepository.save(Category.create(blog.getId(), name, order)));
    }

    @Transactional
    public CategoryResponse rename(Long categoryId, Long memberId, String rawName) {
        Category c = findOwned(categoryId, memberId);
        String name = rawName.trim();
        if (!name.equals(c.getName())) {
            checkName(c.getBlogId(), name);
            c.rename(name);
        }
        return CategoryResponse.of(c);
    }

    /** 지우면 그 카테고리 글은 미분류가 된다 (DB의 ON DELETE SET NULL). */
    @Transactional
    public void delete(Long categoryId, Long memberId) {
        categoryRepository.delete(findOwned(categoryId, memberId));
    }

    /** ids 순서대로 1, 2, 3...을 매긴다. 블로그의 카테고리를 빠짐없이 보내야 한다. */
    @Transactional
    public List<CategoryResponse> reorder(String address, Long memberId, List<Long> ids) {
        Blog blog = blogAccess.findOwned(address, memberId);
        List<Category> all = categoryRepository.findByBlogIdOrderBySortOrderAscIdAsc(blog.getId());
        Map<Long, Category> byId = all.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        if (ids.size() != all.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw ApiException.invalid("ids", "카테고리 목록이 바뀌었어요. 새로고침 후 다시 시도해 주세요");
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).moveTo(i + 1);
        }
        return ids.stream().map(byId::get).map(CategoryResponse::of).toList();
    }

    private Category findOwned(Long categoryId, Long memberId) {
        Category c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("카테고리를 찾을 수 없어요"));
        blogAccess.checkOwner(c.getBlogId(), memberId);
        return c;
    }

    private String checkName(Long blogId, String rawName) {
        String name = rawName.trim();
        if (FIXED_NAMES.contains(name)) {
            throw ApiException.invalid("name", "'" + name + "'은 쓸 수 없는 이름이에요");
        }
        if (categoryRepository.existsByBlogIdAndName(blogId, name)) {
            throw ApiException.invalid("name", "같은 이름의 카테고리가 있어요");
        }
        return name;
    }
}
