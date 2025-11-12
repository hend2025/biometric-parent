package com.biometric.serv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 生物识别业务服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.biometric.serv", "com.biometric.algo"})
@MapperScan("com.biometric.serv.mapper")
@EnableAsync
public class BiometricServApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiometricServApplication.class, args);
    }
}

