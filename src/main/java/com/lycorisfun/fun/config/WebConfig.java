package com.lycorisfun.fun.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration表示是springboot的配置类
@Configuration
public class WebConfig implements WebMvcConfigurer {
//public class WebConfig  {
//    @Autowired
//    private LoginInterceptor loginInterceptor;

//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(loginInterceptor).addPathPatterns("/admin/**").excludePathPatterns("/user/login");
////        registry.addInterceptor(loginInterceptor).addPathPatterns("/admin/**").addPathPatterns("/user/getUserInfo").excludePathPatterns("/user/login");
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**").addResourceLocations("file:D:\\Work\\javaWorkspace\\upload\\img");
    }

}