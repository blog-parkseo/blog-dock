package com.blogdock.post;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/** 글 본문에서 허용한 태그만 남긴다. <script>, onclick 같은 것은 모두 사라진다 (보안 요구사항). */
@Component
public class HtmlSanitizer {

    private static final String BASE = "http://blogdock.invalid";

    private final Safelist safelist = Safelist.relaxed()
            .addTags("del", "s", "hr", "input")
            .addAttributes("input", "type", "checked", "disabled")
            .addAttributes("code", "data-language")
            .addAttributes("pre", "data-language")
            .addProtocols("img", "src", "http", "https")
            .preserveRelativeLinks(true);

    public PostContent clean(String markdown, String html) {
        String safe = Jsoup.clean(html == null ? "" : html, BASE, safelist);
        Document doc = Jsoup.parseBodyFragment(safe);
        for (Element input : doc.select("input")) {
            // 체크박스 목록만 남기고, 눌러서 바꿀 수 없게 한다
            if (!"checkbox".equals(input.attr("type"))) {
                input.remove();
            } else {
                input.attr("disabled", "");
            }
        }
        for (Element a : doc.select("a[href]")) {
            a.attr("rel", "noopener noreferrer nofollow");
        }
        String body = doc.body().html();
        return new PostContent(markdown == null ? "" : markdown, body, doc.body().text());
    }

    /** 본문의 첫 이미지 주소. 없으면 null (POST-07). */
    public String firstImageSrc(String safeHtml) {
        Element img = Jsoup.parseBodyFragment(safeHtml).selectFirst("img[src]");
        return img == null ? null : img.attr("src");
    }

    public boolean hasImage(String safeHtml) {
        return firstImageSrc(safeHtml) != null;
    }
}
