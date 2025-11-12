package com.biometric.serv.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PerformanceMonitorService {

    @Autowired
    @Qualifier("hazelcastInstance")
    private HazelcastInstance hazelcastInstance;

    private final AtomicLong totalRecognitionCount = new AtomicLong(0);
    private final AtomicLong successRecognitionCount = new AtomicLong(0);
    private final AtomicLong failedRecognitionCount = new AtomicLong(0);
    private final AtomicLong totalRecognitionTime = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("性能监控服务初始化完成");
    }

    public void recordRecognition(boolean success, long duration) {
        totalRecognitionCount.incrementAndGet();
        if (success) {
            successRecognitionCount.incrementAndGet();
        } else {
            failedRecognitionCount.incrementAndGet();
        }
        totalRecognitionTime.addAndGet(duration);
    }

    public long getAverageRecognitionTime() {
        long total = totalRecognitionCount.get();
        if (total == 0) {
            return 0;
        }
        return totalRecognitionTime.get() / total;
    }

    public PerformanceMetrics getMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        metrics.setTotalRecognitions(totalRecognitionCount.get());
        metrics.setSuccessRecognitions(successRecognitionCount.get());
        metrics.setFailedRecognitions(failedRecognitionCount.get());
        metrics.setAverageResponseTime(getAverageRecognitionTime());
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        metrics.setUsedMemoryMB(heapUsage.getUsed() / 1024 / 1024);
        metrics.setMaxMemoryMB(heapUsage.getMax() / 1024 / 1024);
        metrics.setMemoryUsagePercent((double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        
        if (hazelcastInstance != null) {
            try {
                IMap<String, ?> faceFeatureMap = hazelcastInstance.getMap("faceFeatureMap");
                metrics.setCachedFaceCount(faceFeatureMap.size());
                metrics.setClusterSize(hazelcastInstance.getCluster().getMembers().size());
            } catch (Exception e) {
                log.warn("获取Hazelcast指标失败", e);
            }
        }
        
        return metrics;
    }

    @Scheduled(fixedRate = 300000)
    public void logPerformanceMetrics() {
        PerformanceMetrics metrics = getMetrics();
        log.info("========== 性能指标 ==========");
        log.info("总识别次数: {}", metrics.getTotalRecognitions());
        log.info("成功次数: {}, 失败次数: {}", 
                metrics.getSuccessRecognitions(), metrics.getFailedRecognitions());
        log.info("平均响应时间: {} ms", metrics.getAverageResponseTime());
        log.info("内存使用: {} MB / {} MB ({} %)", 
                metrics.getUsedMemoryMB(), metrics.getMaxMemoryMB(), 
                String.format("%.2f", metrics.getMemoryUsagePercent()));
        log.info("缓存人脸数量: {}", metrics.getCachedFaceCount());
        log.info("集群节点数: {}", metrics.getClusterSize());
        log.info("===============================");
    }

    @Data
    public static class PerformanceMetrics {
        private long totalRecognitions;
        private long successRecognitions;
        private long failedRecognitions;
        private long averageResponseTime;
        private long usedMemoryMB;
        private long maxMemoryMB;
        private double memoryUsagePercent;
        private int cachedFaceCount;
        private int clusterSize;
    }
}

