package com.sakury.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sakury")
@MapperScan(basePackages = "com.sakury.mappers")
public class VidoraWebRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(VidoraWebRunApplication.class,args);
    }
}
