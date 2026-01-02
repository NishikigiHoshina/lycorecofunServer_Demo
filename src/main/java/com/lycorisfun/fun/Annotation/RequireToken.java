package com.lycorisfun.fun.Annotation;
import java.lang.annotation.*;

@Target(ElementType.METHOD)          // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留，拦截器才能读到
public @interface RequireToken {
}