package com.learnway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImgConfig implements WebMvcConfigurer {

    // 윈도우용
/*    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드한 파일을 '/images/' 경로로 접근할 수 있도록 설정
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///C:/upload/");
    }*/

    // 홈서버 배포용
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드한 파일을 '/images/' 경로로 접근할 수 있도록 설정
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///upload/"); // 리눅스 경로로 수정
    }
}
