package com.blogdock.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.blogdock.image.ImageService;

/**
 * 올린 이미지를 /files/** 주소로 내보낸다. 파일 이름이 매번 새로 만들어지므로 오래 캐시해도 된다.
 * 배포할 때는 화면 빌드 결과(frontend/dist)를 static/ 에 넣어 같은 서버가 화면도 내보낸다.
 */
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

        // 화면 파일. /blog/주소 처럼 파일이 없는 화면 주소는 index.html을 돌려줘 React가 그린다.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource found = super.getResource(path, location);
                        if (found != null) {
                            return found;
                        }
                        boolean screen = !path.startsWith("api/") && !path.contains(".");
                        Resource index = new ClassPathResource("static/index.html");
                        return screen && index.exists() ? index : null;
                    }
                });
    }
}
