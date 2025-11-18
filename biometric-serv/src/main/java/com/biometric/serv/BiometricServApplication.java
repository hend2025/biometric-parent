package com.biometric.serv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.biometric")
@MapperScan("com.biometric.serv.mapper")
public class BiometricServApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiometricServApplication.class, args);
    }

}