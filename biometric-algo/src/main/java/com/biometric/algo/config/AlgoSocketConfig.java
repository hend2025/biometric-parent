package com.biometric.algo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 算法Socket配置类
 * 配置算法引擎的Socket连接参数和默认版本号
 * 
 * 配置项前缀：biometric.algo.socket
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "biometric.algo.socket")
public class AlgoSocketConfig {
    /**
     * 算法引擎Socket服务IP
     */
    private String host = "127.0.0.1";

    /**
     * 算法引擎Socket服务端口
     */
    private int port = 9098;

    /**
     * Socket连接超时时间(毫秒)
     */
    private int timeout = 60000;

    /**
     * 默认算法版本
     */
    private String defaultFaceVersion = "FACE310";
    private String defaultFingerVersion = "FINGER30";
    private String defaultFingerprintVersion = "FINGERPRINT30";

}