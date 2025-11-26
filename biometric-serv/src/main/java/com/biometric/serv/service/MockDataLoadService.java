package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.PersonFaceData;
import com.biometric.algo.service.FaceCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟数据加载服务
 * 生成 N=1000万人，每人1-3张模板；M=20000个分组，每组1000-2000人
 */
@Service
public class MockDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(MockDataLoadService.class);

    // 默认配置
    private static final int DEFAULT_TOTAL_PERSONS = 10_000_00;  // 100万人
    private static final int DEFAULT_TOTAL_GROUPS = 20_00;       // 2万个分组
    private static final int MIN_TEMPLATES_PER_PERSON = 1;
    private static final int MAX_TEMPLATES_PER_PERSON = 3;
    private static final int BATCH_SIZE = 5000;  // 平衡内存和写入次数
    private static final int FEATURE_DATA_SIZE = 512;             // 模拟特征向量字节数

    @Autowired
    private FaceCacheService faceCacheService;

    // 进度跟踪
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
     * 启动模拟数据加载
     */
    public synchronized Map<String, Object> startMockDataLoad(Integer totalPersons, Integer totalGroups, Integer threadCount) {
        Map<String, Object> result = new HashMap<>();

        if (isRunning.get()) {
            result.put("success", false);
            result.put("message", "数据加载任务正在进行中，请等待完成或先停止");
            return result;
        }

        // 设置参数
        totalPersonsTarget = (totalPersons != null && totalPersons > 0) ? totalPersons : DEFAULT_TOTAL_PERSONS;
        totalGroupsTarget = (totalGroups != null && totalGroups > 0) ? totalGroups : DEFAULT_TOTAL_GROUPS;
        int threads = (threadCount != null && threadCount > 0) ? threadCount : Runtime.getRuntime().availableProcessors();

        // 重置状态
        generatedPersons.set(0);
        loadedPersons.set(0);
        startTime = System.currentTimeMillis();
        endTime = 0;
        status = "RUNNING";
        errorMessage = null;
        isRunning.set(true);

        // 创建线程池 - 使用AtomicInteger避免线程名重复
        threadCounter.set(0);
        executorService = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "MockDataLoader-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        // 异步执行加载任务
        CompletableFuture.runAsync(() -> {
            try {
                executeDataLoad(totalPersonsTarget, totalGroupsTarget, threads);
                status = "COMPLETED";
            } catch (Exception e) {
                log.error("模拟数据加载失败", e);
                status = "FAILED";
                errorMessage = e.getMessage();
            } finally {
                endTime = System.currentTimeMillis();
                isRunning.set(false);
                shutdownExecutor();
            }
        });

        result.put("success", true);
        result.put("message", "模拟数据加载任务已启动");
        result.put("totalPersons", totalPersonsTarget);
        result.put("totalGroups", totalGroupsTarget);
        result.put("threadCount", threads);

        return result;
    }

    /**
     * 执行数据加载（优化版：无需预先构建大型映射）
     */
    private void executeDataLoad(int totalPersons, int totalGroups, int threadCount) throws Exception {
        log.info("开始生成模拟数据: {} 人, {} 分组, {} 线程 [内存优化模式]", totalPersons, totalGroups, threadCount);
        log.info("使用确定性算法动态计算分组，无需预先构建映射表");

        // 直接多线程生成并加载人员数据，使用确定性算法动态计算分组
        int personsPerThread = totalPersons / threadCount;
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            final int startIndex = t * personsPerThread;
            final int endIndex = (t == threadCount - 1) ? totalPersons : startIndex + personsPerThread;

            Future<?> future = executorService.submit(() -> {
                generateAndLoadPersonBatch(startIndex, endIndex, totalGroups, threadIndex);
            });
            futures.add(future);
        }

        // 等待所有线程完成
        for (Future<?> future : futures) {
            future.get();
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        log.info("模拟数据加载完成! 总人数: {}, 耗时: {}s, 速率: {} 人/秒",
                loadedPersons.get(), duration, duration > 0 ? loadedPersons.get() / duration : loadedPersons.get());
    }

    /**
     * 使用确定性哈希算法为人员动态计算分组（无需预先构建映射）
     * 每个人分配到1-3个分组，基于personIndex的哈希值确定
     */
    private String[] calculatePersonGroups(int personIndex, int totalGroups) {
        // 使用personIndex作为种子，保证确定性和可重复性
        Random random = new Random(personIndex * 31L + 17L);
        
        // 每个人分配到1-3个分组
        int groupCount = 1 + random.nextInt(3);
        String[] groups = new String[groupCount];
        
        // 使用Set避免重复分组
        Set<Integer> selectedGroups = new HashSet<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            int groupIndex;
            do {
                groupIndex = random.nextInt(totalGroups);
            } while (selectedGroups.contains(groupIndex));
            
            selectedGroups.add(groupIndex);
            groups[i] = String.format("GRP_%08d", groupIndex);
        }
        
        return groups;
    }

    /**
     * 生成并加载一批人员数据（内存优化版：动态计算分组）
     */
    private void generateAndLoadPersonBatch(int startIndex, int endIndex, 
                                            int totalGroups, int threadIndex) {
        Random random = new Random(startIndex); // 使用起始索引作为种子
        List<PersonFaceData> batch = new ArrayList<>(BATCH_SIZE);
        int processedCount = 0;

        for (int i = startIndex; i < endIndex; i++) {
            if (!isRunning.get()) {
                log.warn("线程 {} 收到停止信号，退出处理", threadIndex);
                break;
            }

            // 动态计算该人员的分组，无需查询预构建的映射表
            String[] groups = calculatePersonGroups(i, totalGroups);
            PersonFaceData person = generatePerson(i, groups, random);
            batch.add(person);
            generatedPersons.incrementAndGet();
            processedCount++;

            if (batch.size() >= BATCH_SIZE) {
                loadBatchToCache(batch, threadIndex);
                batch.clear(); // 清空列表，复用对象
                batch = null;  // 帮助GC
                batch = new ArrayList<>(BATCH_SIZE);
            }
        }

        // 处理剩余数据
        if (!batch.isEmpty()) {
            loadBatchToCache(batch, threadIndex);
        }
        batch.clear();
        batch = null;
        
        log.info("线程 {} 完成，共处理 {} 人", threadIndex, processedCount);
    }

    /**
     * 生成单个人员数据（内存优化版）
     */
    private PersonFaceData generatePerson(int personIndex, String[] groups, Random random) {
        PersonFaceData person = new PersonFaceData();
        person.setPersonId(String.format("PSN_%010d", personIndex));

        // 设置分组（直接使用传入的数组，避免额外转换）
        person.setGroupIds(groups);

        // 生成1-3张人脸模板
        int templateCount = MIN_TEMPLATES_PER_PERSON + random.nextInt(MAX_TEMPLATES_PER_PERSON - MIN_TEMPLATES_PER_PERSON + 1);
        List<CachedFaceFeature> features = new ArrayList<>(templateCount);
        
        for (int t = 0; t < templateCount; t++) {
            CachedFaceFeature feature = new CachedFaceFeature();
            feature.setFaceId(String.format("FACE_%010d_%02d", personIndex, t));
            feature.setAlgoType("FACE310");
            feature.setTemplateType("NORMAL");

            // 生成模拟特征数据（每个模板独立生成，避免共享引用）
            byte[] featureData = new byte[FEATURE_DATA_SIZE];
            random.nextBytes(featureData);
            feature.setFeatureData(featureData);

            features.add(feature);
        }

        person.setFeatures(features);
        return person;
    }

    /**
     * 将批量数据加载到缓存（优化版：自适应流控）
     */
    private void loadBatchToCache(List<PersonFaceData> batch, int threadIndex) {
        try {
            faceCacheService.loadFeatures(batch);
            long loaded = loadedPersons.addAndGet(batch.size());

            // 减少日志输出频率：每100万人输出一次（从50万提升到100万）
            if (loaded % 1000000 < BATCH_SIZE) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                double percent = (loaded * 100.0) / totalPersonsTarget;
                long rate = elapsed > 0 ? loaded / elapsed : loaded;
                
                // 输出内存使用情况
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                double memoryUsage = (usedMemory * 100.0) / maxMemory;
                
                log.info("线程 {} | 进度: {}/{} ({}%) | 速率: {} 人/秒 | 已用时: {}s | 堆内存: {}%",
                        threadIndex, loaded, totalPersonsTarget, String.format("%.2f", percent), 
                        rate, elapsed, String.format("%.1f", memoryUsage));
            }
            
            // 自适应流控：根据内存压力动态调整延迟（优化版）
            if (batch.size() >= BATCH_SIZE) {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                double memoryUsage = (usedMemory * 100.0) / maxMemory;
                
                int sleepTime = 20;  // 基础延迟降低到20ms（从50ms优化）
                if (memoryUsage > 90) {
                    sleepTime = 200;  // 内存>90%，延迟200ms
                    // 只在内存压力极高时输出WARN日志，且降低频率
                    if (loaded % 100000 < BATCH_SIZE) {
                        log.warn("线程 {} 内存压力极高: {}%，增加延迟到 {}ms", threadIndex, 
                                String.format("%.1f", memoryUsage), sleepTime);
                    }
                } else if (memoryUsage > 85) {
                    sleepTime = 100;  // 内存>85%，延迟100ms
                } else if (memoryUsage > 75) {
                    sleepTime = 50;  // 内存>75%，延迟50ms
                }
                
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("线程 {} 被中断", threadIndex);
        } catch (Exception e) {
            log.error("线程 {} 加载批次失败: {}", threadIndex, e.getMessage(), e);
        }
    }

    /**
     * 停止加载任务
     */
    public synchronized Map<String, Object> stopMockDataLoad() {
        Map<String, Object> result = new HashMap<>();

        if (!isRunning.get()) {
            result.put("success", false);
            result.put("message", "没有正在运行的加载任务");
            return result;
        }

        isRunning.set(false);
        status = "STOPPING";
        shutdownExecutor();

        result.put("success", true);
        result.put("message", "已发送停止信号");
        return result;
    }

    /**
     * 获取加载进度
     */
    public Map<String, Object> getProgress() {
        Map<String, Object> progress = new HashMap<>();

        progress.put("status", status);
        progress.put("isRunning", isRunning.get());
        progress.put("totalPersonsTarget", totalPersonsTarget);
        progress.put("totalGroupsTarget", totalGroupsTarget);
        progress.put("generatedPersons", generatedPersons.get());
        progress.put("loadedPersons", loadedPersons.get());

        // 计算进度百分比
        double percent = 0;
        if (totalPersonsTarget > 0) {
            percent = (loadedPersons.get() * 100.0) / totalPersonsTarget;
        }
        progress.put("progressPercent", String.format("%.2f", percent));

        // 计算耗时
        long elapsed = 0;
        if (startTime > 0) {
            elapsed = (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
        }
        progress.put("elapsedTimeMs", elapsed);
        progress.put("elapsedTimeSec", elapsed / 1000);

        // 计算速率
        long rate = 0;
        if (elapsed > 1000) {
            rate = loadedPersons.get() * 1000 / elapsed;
        }
        progress.put("loadRatePerSec", rate);

        // 预估剩余时间
        long remainingPersons = totalPersonsTarget - loadedPersons.get();
        long estimatedRemainingSec = (rate > 0) ? remainingPersons / rate : 0;
        progress.put("estimatedRemainingTimeSec", estimatedRemainingSec);

        // 缓存统计
        try {
            progress.put("currentCacheSize", faceCacheService.getFaceFeatureMap().size());
        } catch (Exception e) {
            progress.put("currentCacheSize", -1);
        }

        // 错误信息
        if (errorMessage != null) {
            progress.put("errorMessage", errorMessage);
        }

        return progress;
    }

    /**
     * 关闭线程池
     */
    private void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
