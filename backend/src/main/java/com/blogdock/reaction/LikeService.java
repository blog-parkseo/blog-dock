package com.blogdock.reaction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.BlogAccess;
import com.blogdock.common.ApiException;
import com.blogdock.post.Post;
import com.blogdock.post.PostService;

/** SOC-01 공감. 누르면 추가, 다시 누르면 취소. 수는 저장하지 않고 매번 센다. */
@Service
public class LikeService {

    private final PostLikeRepository likeRepository;
    private final PostService postService;
    private final BlogAccess blogAccess;

    public LikeService(PostLikeRepository likeRepository, PostService postService, BlogAccess blogAccess) {
        this.likeRepository = likeRepository;
        this.postService = postService;
        this.blogAccess = blogAccess;
    }

    @Transactional
    public LikeResponse like(Long postId, Long memberId) {
        Post post = postService.findViewable(postId, memberId);
        if (blogAccess.find(post.getBlogId()).isOwnedBy(memberId)) {
            throw ApiException.badRequest("OWN_POST", "내 글에는 공감할 수 없어요");
        }
        PostLike.Key key = new PostLike.Key(memberId, postId);
        if (!likeRepository.existsById(key)) {
            try {
                likeRepository.saveAndFlush(new PostLike(memberId, postId));
            } catch (DataIntegrityViolationException e) {
                // 동시에 두 번 눌렀다: 이미 하나 있으니 괜찮다
            }
        }
        return new LikeResponse(true, likeRepository.countByPostId(postId));
    }

    @Transactional
    public LikeResponse unlike(Long postId, Long memberId) {
        postService.findViewable(postId, memberId);
        likeRepository.deleteById(new PostLike.Key(memberId, postId));
        return new LikeResponse(false, likeRepository.countByPostId(postId));
    }

    public record LikeResponse(boolean liked, long likeCount) {
    }
}
