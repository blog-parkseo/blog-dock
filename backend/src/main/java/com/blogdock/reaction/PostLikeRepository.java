package com.blogdock.reaction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.Key> {

    long countByPostId(Long postId);
}
