package com.blogdock.post;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.blog.BlogRepository;
import com.blogdock.category.Category;
import com.blogdock.category.CategoryRepository;
import com.blogdock.comment.CommentRepository;
import com.blogdock.common.ApiException;
import com.blogdock.image.ImageService;
import com.blogdock.post.PostDtos.BlogRef;
import com.blogdock.post.PostDtos.CategoryRef;
import com.blogdock.post.PostDtos.HomeResponse;
import com.blogdock.post.PostDtos.Neighbor;
import com.blogdock.post.PostDtos.PageResponse;
import com.blogdock.post.PostDtos.PostDetail;
import com.blogdock.post.PostDtos.PostEdit;
import com.blogdock.post.PostDtos.PostSummary;
import com.blogdock.post.PostDtos.SavedResponse;
import com.blogdock.post.PostDtos.WriteRequest;
import com.blogdock.reaction.PostLike;
import com.blogdock.reaction.PostLikeRepository;
import com.blogdock.tag.TagRepository;
import com.blogdock.tag.TagService;

/** 글 작성·수정·삭제·조회와 모든 글 목록. */
@Service
public class PostService {

    public static final int PAGE_SIZE = 10;
    public static final int HOME_SIZE = 10;
    private static final int EXCERPT_LENGTH = 150;
    private static final Set<String> VISIBILITIES = Set.of(Post.PUBLIC, Post.PRIVATE);

    private final PostRepository postRepository;
    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final CategoryRepository categoryRepository;
    private final TagService tagService;
    private final TagRepository tagRepository;
    private final PostLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final HtmlSanitizer sanitizer;
    private final ImageService imageService;

    public PostService(PostRepository postRepository, BlogRepository blogRepository, BlogAccess blogAccess,
                       CategoryRepository categoryRepository, TagService tagService, TagRepository tagRepository,
                       PostLikeRepository likeRepository, CommentRepository commentRepository,
                       HtmlSanitizer sanitizer, ImageService imageService) {
        this.postRepository = postRepository;
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.categoryRepository = categoryRepository;
        this.tagService = tagService;
        this.tagRepository = tagRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.sanitizer = sanitizer;
        this.imageService = imageService;
    }

    // ---------- 쓰기 ----------

    /** POST-01 발행 / POST-08 임시저장. 내 블로그에 새 글. */
    @Transactional
    public SavedResponse create(Long memberId, WriteRequest req) {
        Blog blog = blogRepository.findByOwnerId(memberId)
                .orElseThrow(() -> ApiException.conflict("BLOG_REQUIRED", "블로그를 먼저 만들어 주세요"));
        Post post = postRepository.save(Post.create(blog.getId()));
        return save(post, blog, memberId, req);
    }

    /** POST-02 수정. 글 주소·처음 발행일·공감·댓글은 그대로다. */
    @Transactional
    public SavedResponse update(Long postId, Long memberId, WriteRequest req) {
        Post post = findPost(postId);
        Blog blog = blogAccess.checkOwner(post.getBlogId(), memberId);
        return save(post, blog, memberId, req);
    }

    private SavedResponse save(Post post, Blog blog, Long memberId, WriteRequest req) {
        boolean publishing = req.publish() || post.isPublished();
        String title = req.title() == null ? "" : req.title().trim();
        PostContent content = sanitizer.clean(req.contentMarkdown(), req.contentHtml());

        // 제목 → 본문 순서로 확인하고 첫 번째 문제만 알린다
        if (publishing && title.isEmpty()) {
            throw ApiException.invalid("title", "제목을 입력해 주세요");
        }
        if (title.length() > 100) {
            throw ApiException.invalid("title", "제목은 100자 이하로 입력해 주세요");
        }
        if (publishing && content.text().isBlank() && !sanitizer.hasImage(content.html())) {
            throw ApiException.invalid("content", "본문을 입력해 주세요");
        }
        String visibility = req.visibility() == null ? Post.PUBLIC : req.visibility();
        if (!VISIBILITIES.contains(visibility)) {
            throw ApiException.invalid("visibility", "공개 범위를 골라 주세요");
        }
        Long categoryId = req.categoryId();
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .filter(c -> c.getBlogId().equals(blog.getId()))
                    .orElseThrow(() -> ApiException.invalid("categoryId", "카테고리를 찾을 수 없어요"));
        }
        List<String> tags = tagService.normalize(req.tags());

        // POST-07: 고른 대표 이미지, 없으면 본문 첫 이미지, 그것도 없으면 대표 이미지 없음
        String thumbnail = req.thumbnailImageId() != null
                ? imageService.findMine(req.thumbnailImageId(), memberId).thumbUrl()
                : imageService.thumbOf(sanitizer.firstImageSrc(content.html()));

        post.write(title, categoryId, visibility, content, thumbnail);
        if (req.publish()) {
            post.publish();
        }
        tagService.replace(blog.getId(), post.getId(), tags);
        imageService.attachImages(memberId, post.getId(), content.html());
        return new SavedResponse(post.getId(), post.getSlug(), post.getStatus(), blog.getAddress());
    }

    /** POST-06 발행 후 공개 범위 바꾸기 (MNG-01에서도 쓴다). */
    @Transactional
    public SavedResponse changeVisibility(Long postId, Long memberId, String visibility) {
        if (visibility == null || !VISIBILITIES.contains(visibility)) {
            throw ApiException.invalid("visibility", "공개 범위를 골라 주세요");
        }
        Post post = findPost(postId);
        Blog blog = blogAccess.checkOwner(post.getBlogId(), memberId);
        post.changeVisibility(visibility);
        return new SavedResponse(post.getId(), post.getSlug(), post.getStatus(), blog.getAddress());
    }

    /** POST-03 삭제. 댓글·공감·태그 연결은 DB가 한 번에 같이 지운다 (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long postId, Long memberId) {
        Post post = findPost(postId);
        blogAccess.checkOwner(post.getBlogId(), memberId);
        postRepository.delete(post);
    }

    // ---------- 읽기 ----------

    @Transactional(readOnly = true)
    public PostEdit edit(Long postId, Long memberId) {
        Post post = findPost(postId);
        Blog blog = blogAccess.checkOwner(post.getBlogId(), memberId);
        return new PostEdit(post.getId(), post.getSlug(), post.getTitle(), post.getContentMarkdown(),
                post.getContentHtml(), post.getCategoryId(), tagRepository.namesOfPost(post.getId()),
                post.getVisibility(), post.getStatus(), post.getThumbnailUrl(), blog.getAddress());
    }

    /** POST-04: 존재 → 소속 블로그 → 주인 여부 → 공개 범위 순서로 판단하고, 볼 수 없으면 404. */
    @Transactional(readOnly = true)
    public PostDetail detail(String address, String slug, Long viewerId) {
        Blog blog = blogAccess.find(address);
        Post post = postRepository.findByBlogIdAndSlug(blog.getId(), slug)
                .orElseThrow(PostService::postNotFound);
        boolean owner = blog.isOwnedBy(viewerId);
        if (!canView(post, owner)) {
            throw postNotFound();
        }
        PageRequest one = PageRequest.of(0, 1);
        Neighbor prev = postRepository.findOlder(blog.getId(), owner, post.getPublishedAt(), post.getId(), one)
                .stream().findFirst().map(PostService::neighbor).orElse(null);
        Neighbor next = postRepository.findNewer(blog.getId(), owner, post.getPublishedAt(), post.getId(), one)
                .stream().findFirst().map(PostService::neighbor).orElse(null);
        boolean liked = viewerId != null && likeRepository.existsById(new PostLike.Key(viewerId, post.getId()));
        return new PostDetail(post.getId(), post.getSlug(), post.getTitle(), post.getContentHtml(),
                post.getVisibility(), post.getPublishedAt(), post.getUpdatedAt(), categoryRef(post.getCategoryId()),
                tagRepository.namesOfPost(post.getId()), post.getThumbnailUrl(),
                likeRepository.countByPostId(post.getId()), liked, commentRepository.countByPostId(post.getId()),
                owner, prev, next, new BlogRef(blog.getAddress(), blog.getName()));
    }

    /**
     * BLOG-03 블로그 메인 / CAT-02 카테고리별 / TAG-02 태그별 / SRCH-01 검색.
     * category: 카테고리 id 또는 "none"(미분류).
     */
    @Transactional(readOnly = true)
    public PageResponse<PostSummary> list(String address, Long viewerId, int page, String category,
                                          String tag, String q) {
        Blog blog = blogAccess.find(address);
        boolean owner = blog.isOwnedBy(viewerId);
        Long categoryId = null;
        boolean uncategorized = false;
        if ("none".equals(category)) {
            uncategorized = true;
        } else if (category != null && !category.isBlank()) {
            categoryId = parseId(category);
            Long id = categoryId;
            categoryRepository.findById(id).filter(c -> c.getBlogId().equals(blog.getId()))
                    .orElseThrow(() -> ApiException.notFound("카테고리를 찾을 수 없어요"));
        }
        String pattern = null;
        if (q != null) {
            String keyword = q.trim();
            if (keyword.isEmpty()) {
                throw ApiException.invalid("q", "검색어를 입력해 주세요");
            }
            pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
        }
        String tagName = tag == null || tag.isBlank() ? null : tag.trim();
        Page<Post> result = postRepository.findBlogPosts(blog.getId(), owner, categoryId, uncategorized,
                tagName, pattern, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        return page(result);
    }

    /** HOME-01 홈 최신 글. 커서로 이어 불러와서 중복·누락이 없다. */
    @Transactional(readOnly = true)
    public HomeResponse home(String cursor) {
        Instant at = null;
        Long id = null;
        if (cursor != null && !cursor.isBlank()) {
            String[] parts = cursor.split("_");
            try {
                at = Instant.parse(parts[0]);
                id = Long.parseLong(parts[1]);
            } catch (RuntimeException e) {
                throw ApiException.invalid("cursor", "잘못된 위치예요");
            }
        }
        List<Post> rows = postRepository.findHome(at, id, PageRequest.of(0, HOME_SIZE + 1));
        boolean more = rows.size() > HOME_SIZE;
        List<Post> items = more ? rows.subList(0, HOME_SIZE) : rows;
        String next = null;
        if (more) {
            Post last = items.get(items.size() - 1);
            next = last.getPublishedAt() + "_" + last.getId();
        }
        return new HomeResponse(summaries(items), next);
    }

    /** MNG-01 내 글 관리. status: PUBLIC / PRIVATE / DRAFT */
    @Transactional(readOnly = true)
    public PageResponse<PostSummary> mine(Long memberId, String status, int page) {
        Blog blog = blogRepository.findByOwnerId(memberId)
                .orElseThrow(() -> ApiException.conflict("BLOG_REQUIRED", "블로그를 먼저 만들어 주세요"));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        Page<Post> result = switch (status == null ? Post.PUBLIC : status) {
            case Post.DRAFT -> postRepository.findByBlogIdAndStatusOrderByUpdatedAtDescIdDesc(
                    blog.getId(), Post.DRAFT, pageable);
            case Post.PUBLIC, Post.PRIVATE -> postRepository.findByBlogIdAndStatusAndVisibilityOrderByPublishedAtDescIdDesc(
                    blog.getId(), Post.PUBLISHED, status, pageable);
            default -> throw ApiException.invalid("status", "PUBLIC, PRIVATE, DRAFT 중 하나를 골라 주세요");
        };
        return page(result);
    }

    /** 글을 볼 수 있는지 한 곳에서 판단한다 (공통 정책 2). 임시저장 글은 주인만. */
    public static boolean canView(Post post, boolean owner) {
        if (owner) {
            return true;
        }
        return post.isPublished() && post.isPublic();
    }

    /** 댓글·공감처럼 글에 딸린 기능이 쓰는 확인. 볼 수 없으면 404. */
    @Transactional(readOnly = true)
    public Post findViewable(Long postId, Long viewerId) {
        Post post = findPost(postId);
        Blog blog = blogAccess.find(post.getBlogId());
        if (!post.isPublished() || !canView(post, blog.isOwnedBy(viewerId))) {
            throw postNotFound();
        }
        return post;
    }

    // ---------- 도우미 ----------

    private PageResponse<PostSummary> page(Page<Post> result) {
        return new PageResponse<>(summaries(result.getContent()), result.getNumber(), result.getTotalPages(),
                result.getTotalElements(), result.hasNext());
    }

    private List<PostSummary> summaries(List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        Map<Long, Blog> blogs = byId(blogRepository.findAllById(
                posts.stream().map(Post::getBlogId).collect(Collectors.toSet())), Blog::getId);
        Map<Long, Category> categories = byId(categoryRepository.findAllById(
                posts.stream().map(Post::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet())),
                Category::getId);
        return posts.stream().map(p -> {
            Blog b = blogs.get(p.getBlogId());
            Category c = p.getCategoryId() == null ? null : categories.get(p.getCategoryId());
            return new PostSummary(p.getId(), p.getSlug(), p.getTitle(), excerpt(p.getContentText()),
                    p.getPublishedAt(), p.getUpdatedAt(), c == null ? null : new CategoryRef(c.getId(), c.getName()),
                    p.getThumbnailUrl(), p.getVisibility(), p.getStatus(), new BlogRef(b.getAddress(), b.getName()));
        }).toList();
    }

    private CategoryRef categoryRef(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId).map(c -> new CategoryRef(c.getId(), c.getName())).orElse(null);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId).orElseThrow(PostService::postNotFound);
    }

    private static Neighbor neighbor(Post p) {
        return new Neighbor(p.getSlug(), p.getTitle());
    }

    private static ApiException postNotFound() {
        return ApiException.notFound("글을 찾을 수 없어요");
    }

    private static String excerpt(String text) {
        if (text == null) {
            return "";
        }
        String t = text.strip();
        return t.length() <= EXCERPT_LENGTH ? t : t.substring(0, EXCERPT_LENGTH) + "…";
    }

    /** LIKE 검색에서 %, _ 를 글자 그대로 찾게 한다. 이스케이프 문자는 '!'. */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static Long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw ApiException.notFound("카테고리를 찾을 수 없어요");
        }
    }

    private static <T> Map<Long, T> byId(Collection<T> items, Function<T, Long> id) {
        Map<Long, T> map = new HashMap<>();
        items.forEach(i -> map.put(id.apply(i), i));
        return map;
    }
}
