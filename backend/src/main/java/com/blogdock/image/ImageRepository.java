package com.blogdock.image;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByOriginalPath(String originalPath);

    List<Image> findByUploaderIdAndOriginalPathIn(Long uploaderId, Collection<String> originalPaths);
}
