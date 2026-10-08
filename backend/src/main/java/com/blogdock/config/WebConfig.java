package com.blogdock.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.blogdock.image.ImageService;

/** 올린 이미지를 /files/** 주소로 내보낸다. 파일 이름이 매번 새로 만들어지므로 오래 캐시해도 된다. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImageService imageService;

    public WebConfig(ImageService imageService) {
        this.imageService = imageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ImageService.URL_PREFIX + "**")
                .addResourceLocations(imageService.root().toUri().toString())
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}
