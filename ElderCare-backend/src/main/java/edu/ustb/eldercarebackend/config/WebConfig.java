package edu.ustb.eldercarebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 配置静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 显式配置静态资源映射（确保头像文件可以正常访问）
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("classpath:/static/uploads/avatars/")
                .setCachePeriod(3600); // 缓存1小时
        
        // 配置其他静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
