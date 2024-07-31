package com.song.componentdevelopment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author song
 * @version 0.0.1
 * @date 2024/7/27 19:25
 */
@Configuration
public class WevMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // 允许访问的源
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的HTTP方法
//                .allowCredentials(true) // 允许发送认证信息
                .maxAge(3600); // 预检请求的有效期，单位秒
    }
}


