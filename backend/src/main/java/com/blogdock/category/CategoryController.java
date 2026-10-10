package com.blogdock.category;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.category.CategoryDtos.CategoryResponse;
import com.blogdock.category.CategoryDtos.NameRequest;
import com.blogdock.category.CategoryDtos.OrderRequest;

import jakarta.validation.Valid;

@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/blogs/{address}/categories")
    public List<CategoryResponse> list(@PathVariable String address) {
        return categoryService.list(address);
    }

    @PostMapping("/api/blogs/{address}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@PathVariable String address, @AuthenticationPrincipal LoginMember login,
                                   @Valid @RequestBody NameRequest req) {
        return categoryService.create(address, LoginMember.require(login), req.name());
    }

    @PutMapping("/api/blogs/{address}/categories/order")
    public List<CategoryResponse> reorder(@PathVariable String address, @AuthenticationPrincipal LoginMember login,
                                          @Valid @RequestBody OrderRequest req) {
        return categoryService.reorder(address, LoginMember.require(login), req.ids());
    }

    @PatchMapping("/api/categories/{id}")
    public CategoryResponse rename(@PathVariable Long id, @AuthenticationPrincipal LoginMember login,
                                   @Valid @RequestBody NameRequest req) {
        return categoryService.rename(id, LoginMember.require(login), req.name());
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        categoryService.delete(id, LoginMember.require(login));
    }
}
