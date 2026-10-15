package com.blogdock.post;

import java.util.Arrays;

import com.blogdock.common.ApiException;

/** HOME-05 사이트 공통 테마. 목록을 바꾸려면 여기와 V8의 CHECK를 같이 고친다. */
public enum Theme {
    DAILY("일상", "☕", "오늘의 일상", "평범한 하루에도 이야기는 있어요.\n소소한 일상을 기록한 글을 모았어요."),
    TRAVEL("여행", "✈️", "떠나고 싶은 날", "주말 나들이부터 먼 여행까지.\n가 보고 싶은 곳을 미리 둘러보세요."),
    FOOD("맛집", "🍜", "오늘 뭐 먹지", "직접 가 보고 쓴 맛집 이야기예요.\n메뉴 고민은 여기서 끝내 보세요."),
    IT("IT", "💻", "개발자의 노트", "공부한 것, 만든 것, 부딪힌 것.\n기술 이야기를 모았어요."),
    BOOK_MOVIE("책·영화", "🎬", "이번 주말엔 뭐 볼까", "읽고 본 것들의 감상을 나눠요.\n다음에 볼 작품을 골라 보세요.");

    private final String label;
    private final String emoji;
    private final String title;
    private final String description;

    Theme(String label, String emoji, String title, String description) {
        this.label = label;
        this.emoji = emoji;
        this.title = title;
        this.description = description;
    }

    /** 비어 있으면 테마 없음(null). 모르는 값이면 400. */
    public static String check(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values()).filter(t -> t.name().equals(code)).findFirst()
                .orElseThrow(() -> ApiException.invalid("theme", "테마를 다시 골라 주세요")).name();
    }

    public String label() {
        return label;
    }

    public String emoji() {
        return emoji;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
