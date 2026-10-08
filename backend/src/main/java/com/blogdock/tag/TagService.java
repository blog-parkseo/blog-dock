package com.blogdock.tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blogdock.blog.Blog;
import com.blogdock.blog.BlogAccess;
import com.blogdock.common.ApiException;

/** TAG-01 태그 달기, TAG-03 태그 목록. */
@Service
public class TagService {

    public static final int MAX_TAGS = 10;

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final BlogAccess blogAccess;

    public TagService(TagRepository tagRepository, PostTagRepository postTagRepository, BlogAccess blogAccess) {
        this.tagRepository = tagRepository;
        this.postTagRepository = postTagRepository;
        this.blogAccess = blogAccess;
    }

    /** 앞뒤 공백과 '#'을 빼고, 대소문자만 다른 것도 같은 태그로 본다. 10개를 넘으면 400. */
    public List<String> normalize(List<String> raw) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String r : raw == null ? List.<String>of() : raw) {
            if (r == null) {
                continue;
            }
            String name = r.trim().replaceFirst("^#+", "").trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.length() > 30) {
                throw ApiException.invalid("tags", "태그는 30자 이하로 입력해 주세요");
            }
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        if (unique.size() > MAX_TAGS) {
            throw ApiException.invalid("tags", "태그는 " + MAX_TAGS + "개까지 달 수 있어요");
        }
        return new ArrayList<>(unique.values());
    }

    /** 글의 태그를 통째로 바꾼다. 블로그에 없던 이름은 새 태그가 된다. */
    @Transactional
    public void replace(Long blogId, Long postId, List<String> names) {
        tagRepository.unlinkAll(postId);
        for (String name : names) {
            Tag tag = tagRepository.findFirstByBlogIdAndNameIgnoreCase(blogId, name)
                    .orElseGet(() -> tagRepository.save(Tag.create(blogId, name)));
            postTagRepository.save(new PostTag(postId, tag.getId()));
        }
    }

    @Transactional(readOnly = true)
    public List<TagCount> counts(String address, Long viewerId) {
        Blog blog = blogAccess.find(address);
        return tagRepository.countsOfBlog(blog.getId(), blog.isOwnedBy(viewerId)).stream()
                .map(row -> new TagCount((String) row[0], (Long) row[1]))
                .toList();
    }

    public record TagCount(String name, long count) {
    }
}
