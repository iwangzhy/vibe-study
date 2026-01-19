package com.vibe.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类
 */
@EnableDiscoveryClient
@MapperScan("com.vibe.user.mapper")
@SpringBootApplication(scanBasePackages = {"com.vibe.user", "com.vibe.common"})
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
        System.out.println("========== User Service Started ==========");
    }
}
