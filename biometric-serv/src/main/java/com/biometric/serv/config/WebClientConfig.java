package com.biometric.serv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient配置 - 用于调用算法服务
 */
@Configuration
public class WebClientConfig {

    @Value("${biometric.algo.url:http://localhost:8081}")
    private String algoServiceUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(algoServiceUrl)
                .build();
    }
}

