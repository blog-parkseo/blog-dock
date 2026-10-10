package com.blogdock.blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservedWordRepository extends JpaRepository<ReservedWord, String> {
}
