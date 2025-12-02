package com.biometric.algo.service;

import com.biometric.algo.config.HazelcastConfiguration;
import com.biometric.algo.dto.PersonFaceData;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class FaceCacheService {
    private static final Logger log = LoggerFactory.getLogger(FaceCacheService.class);

    // 优化批次大小平衡网络交互和CPU负载
    private static final int SUB_BATCH_SIZE = 1000;
    // 限制并发写入数量，防止内存过载（降低以减少内存压力）
    private static final int MAX_CONCURRENT_WRITES = 4;
    private final Semaphore writeSemaphore = new Semaphore(MAX_CONCURRENT_WRITES);
    
    // 内存压力阈值（堆使用率超过此值时暂停写入）
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.85;
    private static final long MEMORY_CHECK_INTERVAL_MS = 500;
    private final IMap<String, PersonFaceData> faceFeatureMap;

    @Autowired
    public FaceCacheService(HazelcastInstance hazelcastInstance) {
        this.faceFeatureMap = hazelcastInstance.getMap(HazelcastConfiguration.FACE_FEATURE_MAP);
    }

    public void loadFeatures(List<PersonFaceData> features) {
        if (features == null || features.isEmpty()) return;

        // 如果批次正好合适，直接转 Map 写入，避免 subList 创建的开销
        if (features.size() <= SUB_BATCH_SIZE) {
            putAllToHazelcast(features);
        } else {
            // 大批次拆分
            int totalSize = features.size();
            for (int i = 0; i < totalSize; i += SUB_BATCH_SIZE) {
                int end = Math.min(i + SUB_BATCH_SIZE, totalSize);
                putAllToHazelcast(features.subList(i, end));
            }
        }
    }

    private void putAllToHazelcast(List<PersonFaceData> batch) {
        try {
            // 内存压力检测：如果堆使用率过高，等待GC
            waitForMemoryAvailable();
            
            // 获取写入许可，实现背压控制
            writeSemaphore.acquire();
            
            try {
                // 指定 HashMap 大小，避免 resize 扩容开销
                Map<String, PersonFaceData> batchMap = new HashMap<>((int)(batch.size() / 0.75) + 1);

                for (PersonFaceData data : batch) {
                    batchMap.put(data.getPersonId(), data);
                }

                // 同步写入，避免异步操作堆积导致OOM
                // 在大批量数据加载场景下，同步写入更稳定
                faceFeatureMap.putAll(batchMap);
                
            } finally {
                // 确保释放信号量
                writeSemaphore.release();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Hazelcast 写入被中断", e);
        } catch (Exception e) {
            log.error("Hazelcast 批量写入失败", e);
        }
    }
    
    /**
     * 等待内存可用
     * 当堆使用率超过阈值时，暂停并让出CPU，让GC有机会运行
     */
    private void waitForMemoryAvailable() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        while (true) {
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double usageRatio = (double) usedMemory / maxMemory;
            
            if (usageRatio < MEMORY_PRESSURE_THRESHOLD) {
                break;
            }
            
            // 内存压力过大，让出CPU让GC运行（避免显式调用System.gc()）
            log.warn("内存压力过大 ({}%)，暂停写入等待内存释放...", String.format("%.1f", usageRatio * 100));
            System.gc();

            try {
                Thread.sleep(MEMORY_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void clearCache() {
        faceFeatureMap.clear();
    }

    public IMap<String, PersonFaceData> getFaceFeatureMap() {
        return faceFeatureMap;
    }

}