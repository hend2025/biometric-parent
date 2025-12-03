package com.biometric.serv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 服务器配置自动检测和优化策略
 * 根据 CPU 核心数和内存大小自动调整线程池和批处理参数
 */
public class ServerConfigOptimizer {
    
    private static final Logger log = LoggerFactory.getLogger(ServerConfigOptimizer.class);
    
    private final int cpuCores;
    private final long maxMemoryGB;
    private final LoaderConfig loaderConfig;
    
    public ServerConfigOptimizer() {
        this(null, null, null, null, null, null);
    }
    
    /**
     * 构造函数，支持从配置文件传入参数
     * @param coreThreads 核心线程数，null则自动计算
     * @param maxThreads 最大线程数，null则自动计算
     * @param batchSize 批处理大小，null则自动计算
     * @param queueSize 队列大小，null则自动计算
     * @param dbFetchSize 数据库拉取大小，null则自动计算
     * @param logInterval 日志间隔，null则自动计算
     */
    public ServerConfigOptimizer(Integer coreThreads, Integer maxThreads, Integer batchSize, 
                                  Integer queueSize, Integer dbFetchSize, Integer logInterval) {
        this.cpuCores = Runtime.getRuntime().availableProcessors();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        this.maxMemoryGB = heapMemoryUsage.getMax() / (1024 * 1024 * 1024);
        
        this.loaderConfig = computeOptimalConfig(coreThreads, maxThreads, batchSize, 
                                                   queueSize, dbFetchSize, logInterval);
        logConfiguration();
    }
    
    /**
     * 根据服务器配置计算最优参数
     * 如果传入的参数不为null，则使用配置值；否则自动计算
     */
    private LoaderConfig computeOptimalConfig(Integer configCoreThreads, Integer configMaxThreads, 
                                                Integer configBatchSize, Integer configQueueSize,
                                                Integer configDbFetchSize, Integer configLogInterval) {
        LoaderConfig config = new LoaderConfig();
        
        // 根据不同的服务器配置计算参数
        int autoCoreThreads;
        int autoMaxThreads;
        int autoBatchSize;
        int autoQueueSize;
        int autoDbFetchSize;
        int autoLogInterval;
        
        if (cpuCores <= 8 && maxMemoryGB <= 20) {
            // 8核/16G - 保守配置，避免内存压力
            autoCoreThreads = Math.max(4, cpuCores / 2);
            autoMaxThreads = cpuCores;
            autoBatchSize = 500;  // 减小批处理大小
            autoQueueSize = 50;   // 减小队列深度
            autoDbFetchSize = 500;
            autoLogInterval = 25000;

        } else if (cpuCores <= 16 && maxMemoryGB <= 40) {
            // 16核/32G - 标准配置
            autoCoreThreads = cpuCores;
            autoMaxThreads = (int)(cpuCores * 1.5);
            autoBatchSize = 1000; // 标准批处理大小
            autoQueueSize = 100;
            autoDbFetchSize = 1000;
            autoLogInterval = 50000;

        } else {
            // 32核/64G - 高性能配置
            autoCoreThreads = cpuCores;
            autoMaxThreads = cpuCores * 2;
            autoBatchSize = 2000; // 增大批处理大小，减少任务提交开销
            autoQueueSize = 200;  // 增大队列深度
            autoDbFetchSize = 2000;
            autoLogInterval = 100000;
        }
        
        // 动态调整：如果实际内存远超预期，适当增加批处理大小
        if (maxMemoryGB > 50) {
            autoBatchSize = Math.min(autoBatchSize * 2, 3000);
            autoQueueSize = Math.min(autoQueueSize * 2, 300);
        }
        
        // 使用配置值或自动计算值
        config.coreThreads = configCoreThreads != null ? configCoreThreads : autoCoreThreads;
        config.maxThreads = configMaxThreads != null ? configMaxThreads : autoMaxThreads;
        config.batchSize = configBatchSize != null ? configBatchSize : autoBatchSize;
        config.queueSize = configQueueSize != null ? configQueueSize : autoQueueSize;
        config.dbFetchSize = configDbFetchSize != null ? configDbFetchSize : autoDbFetchSize;
        config.logInterval = configLogInterval != null ? configLogInterval : autoLogInterval;
        
        return config;
    }
    
    /**
     * 打印配置信息
     */
    private void logConfiguration() {
        log.info("========================================");
        log.info("服务器配置检测结果:");
        log.info("  CPU 核心数: {}", cpuCores);
        log.info("  最大堆内存: {} GB", maxMemoryGB);
        log.info("========================================");
        log.info("服务器优化参数 (配置文件值优先，否则自动计算):");
        log.info("  核心线程数: {}", loaderConfig.coreThreads);
        log.info("  最大线程数: {}", loaderConfig.maxThreads);
        log.info("  批处理大小: {}", loaderConfig.batchSize);
        log.info("  队列深度: {}", loaderConfig.queueSize);
        log.info("  数据库拉取大小: {}", loaderConfig.dbFetchSize);
        log.info("  日志间隔: {}", loaderConfig.logInterval);
        log.info("========================================");
    }
    
    /**
     * 获取当前内存使用情况
     */
    public MemoryStats getCurrentMemoryStats() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        
        MemoryStats stats = new MemoryStats();
        stats.usedMemoryMB = heapUsage.getUsed() / (1024 * 1024);
        stats.maxMemoryMB = heapUsage.getMax() / (1024 * 1024);
        stats.committedMemoryMB = heapUsage.getCommitted() / (1024 * 1024);
        stats.usagePercent = (stats.usedMemoryMB * 100.0) / stats.maxMemoryMB;
        
        return stats;
    }
    
    /**
     * 检查内存压力，返回是否应该暂停加载
     */
    public boolean shouldThrottle() {
        MemoryStats stats = getCurrentMemoryStats();
        
        // 如果内存使用超过80%，建议减速
        if (stats.usagePercent > 80) {
            log.warn("内存使用率过高: {:.2f}%，建议暂缓数据加载", stats.usagePercent);
            return true;
        }
        
        // 如果内存使用超过85%，强制GC
        if (stats.usagePercent > 85) {
            log.error("内存使用率严重过高: {:.2f}%，执行 GC...", stats.usagePercent);
            System.gc();
            return true;
        }
        
        return false;
    }
    
    public LoaderConfig getLoaderConfig() {
        return loaderConfig;
    }

    /**
     * 加载器配置参数
     */
    public static class LoaderConfig {
        public int coreThreads;
        public int maxThreads;
        public int batchSize;
        public int queueSize;
        public int dbFetchSize;
        public int logInterval;
    }
    
    /**
     * 内存统计信息
     */
    public static class MemoryStats {
        public long usedMemoryMB;
        public long maxMemoryMB;
        public long committedMemoryMB;
        public double usagePercent;
        
        @Override
        public String toString() {
            return String.format("内存使用: %d MB / %d MB (%.2f%%)", 
                usedMemoryMB, maxMemoryMB, usagePercent);
        }
    }
    
}
