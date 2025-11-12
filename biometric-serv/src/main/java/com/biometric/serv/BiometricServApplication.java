package com.biometric.serv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.biometric.serv", "com.biometric.algo"})
@MapperScan("com.biometric.serv.mapper")
@EnableAsync
@EnableScheduling
public class BiometricServApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiometricServApplication.class, args);
    }
}
