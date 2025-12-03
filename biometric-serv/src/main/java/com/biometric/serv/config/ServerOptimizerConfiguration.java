package com.biometric.serv.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 服务器优化配置
 * 从配置文件读取 biometric.server-optimizer 配置
 */
@Configuration
@ConfigurationProperties(prefix = "biometric.server-optimizer")
public class ServerOptimizerConfiguration {
    
    private Integer coreThreads;
    private Integer maxThreads;
    private Integer batchSize;
    private Integer queueSize;
    private Integer dbFetchSize;
    private Integer logInterval;
    
    @Bean
    public ServerConfigOptimizer serverConfigOptimizer() {
        return new ServerConfigOptimizer(
            coreThreads,
            maxThreads,
            batchSize,
            queueSize,
            dbFetchSize,
            logInterval
        );
    }

}
