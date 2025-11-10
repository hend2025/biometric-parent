package com.biometric.serv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 生物识别业务服务启动类
 */
@SpringBootApplication
@MapperScan("com.biometric.serv.mapper")
public class BiometricServApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiometricServApplication.class, args);
    }
}

