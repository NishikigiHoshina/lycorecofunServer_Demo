package com.lycorisfun.fun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lycorisfun.fun.Mapper")   // 关键：扫 Mapper 接口
public class LycorisfunServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LycorisfunServerApplication.class, args);
    }

}
