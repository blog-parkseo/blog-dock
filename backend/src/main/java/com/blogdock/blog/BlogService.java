package com.blogdock.blog;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.BlogDtos.AddressCheckResponse;
import com.blogdock.blog.BlogDtos.BlogResponse;
import com.blogdock.blog.BlogDtos.CreateRequest;
import com.blogdock.blog.BlogDtos.UpdateRequest;
import com.blogdock.common.ApiException;

@Service
public class BlogService {

    /** 영문 소문자·숫자·하이픈 4~32자, 하이픈으로 시작하거나 끝나지 않는다. */
    private static final Pattern ADDRESS = Pattern.compile("^[a-z0-9][a-z0-9-]{2,30}[a-z0-9]$");

    private final BlogRepository blogRepository;
    private final ReservedWordRepository reservedWordRepository;

    public BlogService(BlogRepository blogRepository, ReservedWordRepository reservedWordRepository) {
        this.blogRepository = blogRepository;
        this.reservedWordRepository = reservedWordRepository;
    }

    /** BLOG-01: 입력 단계에서 주소를 미리 검사한다. */
    @Transactional(readOnly = true)
    public AddressCheckResponse checkAddress(String address) {
        String reason = addressProblem(address);
        return new AddressCheckResponse(reason == null, reason);
    }

    /** BLOG-01: 블로그 개설. 회원당 1개. */
    @Transactional
    public BlogResponse create(Long memberId, CreateRequest req) {
        if (blogRepository.existsByOwnerId(memberId)) {
            throw ApiException.conflict("BLOG_ALREADY_EXISTS", "이미 블로그가 있어요");
        }
        String address = req.address().trim();
        String problem = addressProblem(address);
        if (problem != null) {
            throw ApiException.invalid("address", messageOf(problem));
        }
        Blog blog = blogRepository.save(Blog.open(memberId, address, req.name().trim(), blankToNull(req.description())));
        return BlogResponse.of(blog, memberId);
    }

    /** BLOG-02, BLOG-03: 블로그 정보. 없으면 404. */
    @Transactional(readOnly = true)
    public BlogResponse get(String address, Long viewerId) {
        return BlogResponse.of(find(address), viewerId);
    }

    /** BLOG-02: 주인만 이름·소개를 바꾼다. 주소는 받지 않는다. */
    @Transactional
    public BlogResponse update(String address, Long memberId, UpdateRequest req) {
        Blog blog = find(address);
        if (!blog.isOwnedBy(memberId)) {
            throw ApiException.forbidden("내 블로그만 고칠 수 있어요");
        }
        blog.update(req.name().trim(), blankToNull(req.description()), blankToNull(req.profileImageUrl()));
        return BlogResponse.of(blog, memberId);
    }

    private Blog find(String address) {
        return blogRepository.findByAddress(address)
                .orElseThrow(() -> ApiException.notFound("블로그를 찾을 수 없어요"));
    }

    private String addressProblem(String address) {
        if (address == null || !ADDRESS.matcher(address).matches()) {
            return "FORMAT";
        }
        if (reservedWordRepository.existsById(address)) {
            return "RESERVED";
        }
        if (blogRepository.existsByAddress(address)) {
            return "TAKEN";
        }
        return null;
    }

    private String messageOf(String problem) {
        return switch (problem) {
            case "FORMAT" -> "주소는 영문 소문자·숫자·하이픈으로 4~32자, 하이픈으로 시작하거나 끝날 수 없어요";
            case "RESERVED" -> "쓸 수 없는 주소예요";
            default -> "이미 사용 중인 주소예요";
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
