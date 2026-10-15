package com.blogdock.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByBlogIdOrderBySortOrderAscIdAsc(Long blogId);

    boolean existsByBlogIdAndName(Long blogId, String name);

    @Query("select coalesce(max(c.sortOrder), 0) from Category c where c.blogId = :blogId")
    int maxSortOrder(Long blogId);
}
