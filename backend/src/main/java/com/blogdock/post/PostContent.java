package com.blogdock.post;

/** 정화를 마친 본문 세 가지 모양. */
public record PostContent(String markdown, String html, String text) {
}
