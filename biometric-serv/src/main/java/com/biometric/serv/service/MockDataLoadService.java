package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.PersonFaceData;
import com.biometric.algo.service.FaceCacheService;
import com.biometric.algo.util.Face303JavaCalcuater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟数据加载服务 (高性能优化版)
 * 目标：极速生成 1000万+ 数据并加载到 Hazelcast
 */
@Service
public class MockDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(MockDataLoadService.class);

    private static final int DEFAULT_TOTAL_PERSONS = 10_000_000;
    private static final int DEFAULT_TOTAL_GROUPS = 20_000;
    private static final int BATCH_SIZE = 500; // 降低批次大小以减少内存压力

    // --- 核心优化：特征资源池 ---
    // 预生成 1000 组标准特征，所有模拟人员复用这些数组引用
    // 这将内存占用从 10GB 降低到 仅需存储对象引用
    private static final int FEATURE_POOL_SIZE = 1000;
    private static final List<MockFeatureTemplate> FEATURE_POOL = new ArrayList<>(FEATURE_POOL_SIZE);

    @Autowired
    private FaceCacheService faceCacheService;

    // 状态跟踪
    private final AtomicLong generatedPersons = new AtomicLong(0);
    private final AtomicLong loadedPersons = new AtomicLong(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private volatile long startTime = 0;
    private volatile long endTime = 0;
    private volatile int totalPersonsTarget = 0;
    private volatile int totalGroupsTarget = 0;
    private volatile String status = "IDLE";
    private volatile String errorMessage = null;

    private ExecutorService executorService;

    /**
     * 服务初始化时预热特征池
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化模拟特征池 (size={})...", FEATURE_POOL_SIZE);
        Random random = new Random(12345); // 固定种子
        for (int i = 0; i < FEATURE_POOL_SIZE; i++) {
            byte[] raw = new byte[512];
            random.nextBytes(raw);

            // 预计算，模拟生产环境的优化结构
            int[] binary = Face303JavaCalcuater.getBinaFeat(raw);
            float[] vector = Face303JavaCalcuater.toFloatArray(raw);

            FEATURE_POOL.add(new MockFeatureTemplate(binary, vector));
        }
        log.info("特征池初始化完成");
    }

    // 简单的特征模板类
    private static class MockFeatureTemplate {
        final int[] binaryFeature;
        final float[] featureVector;

        public MockFeatureTemplate(int[] binaryFeature, float[] featureVector) {
            this.binaryFeature = binaryFeature;
            this.featureVector = featureVector;
        }
    }

    public synchronized Map<String, Object> startMockDataLoad(Integer totalPersons, Integer totalGroups, Integer threadCount) {
        Map<String, Object> result = new HashMap<>();

        if (isRunning.get()) {
            result.put("success", false);
            result.put("message", "任务进行中");
            return result;
        }

        totalPersonsTarget = (totalPersons != null && totalPersons > 0) ? totalPersons : DEFAULT_TOTAL_PERSONS;
        totalGroupsTarget = (totalGroups != null && totalGroups > 0) ? totalGroups : DEFAULT_TOTAL_GROUPS;
        // 降低线程数以减少内存压力和并发竞争
        int threads = (threadCount != null && threadCount > 0) ? threadCount : Math.min(Runtime.getRuntime().availableProcessors(), 4);

        generatedPersons.set(0);
        loadedPersons.set(0);
        startTime = System.currentTimeMillis();
        status = "RUNNING";
        isRunning.set(true);

        executorService = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "MockLoader-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        CompletableFuture.runAsync(() -> {
            try {
                executeDataLoad(totalPersonsTarget, totalGroupsTarget, threads);
                status = "COMPLETED";
            } catch (Exception e) {
                log.error("模拟加载异常", e);
                status = "FAILED";
                errorMessage = e.getMessage();
            } finally {
                endTime = System.currentTimeMillis();
                isRunning.set(false);
                shutdownExecutor();
            }
        });

        result.put("success", true);
        result.put("message", "极速模拟加载已启动");
        result.put("target", totalPersonsTarget);
        return result;
    }

    private void executeDataLoad(int totalPersons, int totalGroups, int threadCount) throws Exception {
        int personsPerThread = totalPersons / threadCount;
        int remaining = totalPersons % threadCount;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            final int start = t * personsPerThread;
            final int end = start + personsPerThread + (t == threadCount - 1 ? remaining : 0);

            futures.add(CompletableFuture.runAsync(() ->
                    generateAndLoadBatch(start, end, totalGroups, threadIndex), executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        log.info("加载完成! 总数: {}, 耗时: {}s", loadedPersons.get(), duration);
    }

    private void generateAndLoadBatch(int start, int end, int totalGroups, int threadIndex) {
        // 使用 ThreadLocalRandom 避免并发竞争
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<PersonFaceData> batch = new ArrayList<>(BATCH_SIZE);

        // 预先计算常用的字符串前缀，减少 StringBuilder 压力
        String threadPrefix = "P" + threadIndex + "_";

        for (int i = start; i < end; i++) {
            if (!isRunning.get()) break;

            PersonFaceData person = new PersonFaceData();
            // 优化：字符串拼接比 String.format 快几十倍
            person.setPersonId(threadPrefix + i);
            person.setGroupIds(calculateGroups(i, totalGroups, random));

            // 每个人 1-2 张人脸
            int templateCount = 1 + random.nextInt(2);
            List<CachedFaceFeature> features = new ArrayList<>(templateCount);

            for (int k = 0; k < templateCount; k++) {
                CachedFaceFeature feature = new CachedFaceFeature();
                feature.setFaceId(person.getPersonId() + "_" + k);
                feature.setAlgoType("FACE310");
                feature.setTemplateType("NORMAL");

                // 核心优化：从池中随机取一个引用，而不是创建新数组
                MockFeatureTemplate template = FEATURE_POOL.get(random.nextInt(FEATURE_POOL_SIZE));

                // 模拟 DataLoadService 的优化结构：设置预计算字段，featuresData设为null
                feature.setBinaryFeature(template.binaryFeature);
                feature.setFeatureVector(template.featureVector);
                feature.setFeatureData(null); // 省内存

                features.add(feature);
            }
            person.setFeatures(features);
            batch.add(person);

            if (batch.size() >= BATCH_SIZE) {
                flushBatch(batch, threadIndex);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) flushBatch(batch, threadIndex);
    }

    private String[] calculateGroups(int seed, int totalGroups, ThreadLocalRandom random) {
        // 简单快速的分组逻辑
        int g1 = random.nextInt(totalGroups);
        // 30% 概率有第二个分组
        if (random.nextFloat() > 0.7) {
            return new String[]{"GRP_" + g1, "GRP_" + random.nextInt(totalGroups)};
        }
        return new String[]{"GRP_" + g1};
    }

    private void flushBatch(List<PersonFaceData> batch, int threadIndex) {
        try {
            faceCacheService.loadFeatures(batch);
            long total = loadedPersons.addAndGet(batch.size());

            // 降低日志频率，每 50万 条打印一次，减少IO影响
            if (total % 500_000 < BATCH_SIZE) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000 + 1;
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                long maxMemory = runtime.maxMemory() / (1024 * 1024);
                log.info("已加载: {} ({} 人/秒) [线程{}] [内存: {}MB/{}MB]", 
                    total, total / elapsed, threadIndex, usedMemory, maxMemory);
            }
        } catch (Exception e) {
            log.error("写入失败", e);
        }
    }

    private void shutdownExecutor() {
        if (executorService != null) executorService.shutdownNow();
    }

    // ... 其他 getProgress, stop 方法保持不变 ...
    public synchronized Map<String, Object> stopMockDataLoad() {
        isRunning.set(false);
        shutdownExecutor();
        return Collections.singletonMap("success", true);
    }

    public Map<String, Object> getProgress() {
        Map<String, Object> map = new HashMap<>();
        map.put("loaded", loadedPersons.get());
        map.put("target", totalPersonsTarget);
        map.put("status", status);
        return map;
    }

}