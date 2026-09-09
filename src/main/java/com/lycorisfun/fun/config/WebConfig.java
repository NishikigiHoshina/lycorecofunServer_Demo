package com.lycorisfun.fun.config;

import com.lycorisfun.fun.Interceptor.TokenInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

//@Configuration表示是springboot的配置类
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UploadProperties uploadProps;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenInterceptor())
                .addPathPatterns("/api/**");   // 所有接口都走，内部按注解区分
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 方案 B：带凭证(cookie)跨源 → origin 不能是 “*”
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * 按资源类型注册静态映射：{urlPath}/** → {root}/{dir}。
     * 目录与对外 URL 均来自配置，不再硬编码。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (UploadProperties.Type t : uploadProps.activeTypes()) {
            Path abs = Paths.get(uploadProps.getRoot()).resolve(t.getDir()).toAbsolutePath().normalize();
            String location = "file:" + abs + "/";
            registry.addResourceHandler(t.getUrlPath() + "/**")
                    .addResourceLocations(location);
        }
    }

    /** 上传静态资源统一加安全响应头，避免内容嗅探执行（nosniff） */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> uploadSecurityHeaders() {
        FilterRegistrationBean<OncePerRequestFilter> reg = new FilterRegistrationBean<>();
        for (UploadProperties.Type t : uploadProps.activeTypes()) {
            reg.addUrlPatterns(t.getUrlPath() + "/*");
        }
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            FilterChain chain) throws ServletException, IOException {
                resp.setHeader("X-Content-Type-Options", "nosniff");
                chain.doFilter(req, resp);
            }
        });
        return reg;
    }
}
