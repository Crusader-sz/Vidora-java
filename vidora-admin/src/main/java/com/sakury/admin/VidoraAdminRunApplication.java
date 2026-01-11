package com.sakury.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.sakury",exclude = {DataSourceAutoConfiguration.class})
public class VidoraAdminRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(VidoraAdminRunApplication.class, args);
    }
}
