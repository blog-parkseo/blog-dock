package com.blogdock.blog;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findByAddress(String address);

    Optional<Blog> findByOwnerId(Long ownerId);

    boolean existsByAddress(String address);

    boolean existsByOwnerId(Long ownerId);
}
