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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 模拟数据加载服务
 * 生成 N=1000万人，每人1-3张模板；M=20000个分组，每组1000-2000人
 */
@Service
public class MockDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(MockDataLoadService.class);

    // 默认配置
    private static final int DEFAULT_TOTAL_PERSONS = 10_000_000;  // 1000万人
    private static final int DEFAULT_TOTAL_GROUPS = 20_000;       // 2万个分组
    private static final int MIN_TEMPLATES_PER_PERSON = 1;
    private static final int MAX_TEMPLATES_PER_PERSON = 3;
    private static final int MIN_PERSONS_PER_GROUP = 1000;
    private static final int MAX_PERSONS_PER_GROUP = 3000;
    private static final int BATCH_SIZE = 10000;
    private static final int FEATURE_DATA_SIZE = 512;             // 模拟特征向量字节数

    @Autowired
    private FaceCacheService faceCacheService;

    // 进度跟踪
    private final AtomicLong generatedPersons = new AtomicLong(0);
    private final AtomicLong loadedPersons = new AtomicLong(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
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

        // 创建线程池
        executorService = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "MockDataLoader-" + System.currentTimeMillis());
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
     * 执行数据加载
     */
    private void executeDataLoad(int totalPersons, int totalGroups, int threadCount) throws Exception {
        log.info("开始生成模拟数据: {} 人, {} 分组, {} 线程", totalPersons, totalGroups, threadCount);

        // Step 1: 预先生成分组->人员映射关系
        log.info("Step 1: 生成分组人员分配...");
        Map<String, List<Integer>> groupToPersonIndices = generateGroupAssignments(totalPersons, totalGroups);
        log.info("人员分组映射构建完成，共 {} 分组", groupToPersonIndices.size());
        
        // Step 2: 反转为人员->分组映射（优化版）
        log.info("Step 2: 构建人员分组映射...");
        Map<Integer, Set<String>> personToGroups = new ConcurrentHashMap<>(totalPersons / 10);
        
        // 并行处理分组映射反转
        groupToPersonIndices.entrySet().parallelStream().forEach(entry -> {
            String groupId = entry.getKey();
            for (Integer personIndex : entry.getValue()) {
                personToGroups.computeIfAbsent(personIndex, k -> ConcurrentHashMap.newKeySet()).add(groupId);
            }
        });
        
        log.info("人员分组映射构建完成，共 {} 人有分组", personToGroups.size());

        // Step 3: 多线程生成并加载人员数据
        log.info("Step 3: 多线程生成人员数据...");
        int personsPerThread = totalPersons / threadCount;
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            final int startIndex = t * personsPerThread;
            final int endIndex = (t == threadCount - 1) ? totalPersons : startIndex + personsPerThread;

            Future<?> future = executorService.submit(() -> {
                generateAndLoadPersonBatch(startIndex, endIndex, personToGroups, threadIndex);
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
     * 生成分组人员分配（优化版）
     * 使用更高效的策略：每个人分配到1-3个随机分组
     */
    private Map<String, List<Integer>> generateGroupAssignments(int totalPersons, int totalGroups) {
        Map<String, List<Integer>> groupToPersons = new ConcurrentHashMap<>(totalGroups);
        
        // 预初始化所有分组，避免并发修改
        for (int g = 0; g < totalGroups; g++) {
            String groupId = String.format("GRP_%08d", g);
            // 预估每个分组约1500人，预分配容量
            groupToPersons.put(groupId, new ArrayList<>(1800));
        }
        
        Random random = new Random(42);

        // 为每个人分配到1-3个随机分组（更符合实际场景且更高效）
        for (int personIndex = 0; personIndex < totalPersons; personIndex++) {
            int groupCount = 1 + random.nextInt(3); // 1-3个分组
            for (int i = 0; i < groupCount; i++) {
                int groupIndex = random.nextInt(totalGroups);
                String groupId = String.format("GRP_%08d", groupIndex);
                groupToPersons.get(groupId).add(personIndex);
            }
            
            if ((personIndex + 1) % 1000000 == 0) {
                log.info("已分配 {}/{} 人到分组", personIndex + 1, totalPersons);
            }
        }

        return groupToPersons;
    }

    /**
     * 生成并加载一批人员数据（优化版）
     */
    private void generateAndLoadPersonBatch(int startIndex, int endIndex, 
                                            Map<Integer, Set<String>> personToGroups, int threadIndex) {
        Random random = new Random(startIndex); // 使用起始索引作为种子
        List<PersonFaceData> batch = new ArrayList<>(BATCH_SIZE);
        int processedCount = 0;

        for (int i = startIndex; i < endIndex; i++) {
            if (!isRunning.get()) {
                log.warn("线程 {} 收到停止信号，退出处理", threadIndex);
                break;
            }

            PersonFaceData person = generatePerson(i, personToGroups.get(i), random);
            batch.add(person);
            generatedPersons.incrementAndGet();
            processedCount++;

            if (batch.size() >= BATCH_SIZE) {
                loadBatchToCache(batch, threadIndex);
                batch = new ArrayList<>(BATCH_SIZE); // 创建新列表而不是clear，减少内存碎片
            }
        }

        // 处理剩余数据
        if (!batch.isEmpty()) {
            loadBatchToCache(batch, threadIndex);
        }
        
        log.info("线程 {} 完成，共处理 {} 人", threadIndex, processedCount);
    }

    /**
     * 生成单个人员数据（优化版）
     */
    private PersonFaceData generatePerson(int personIndex, Set<String> groups, Random random) {
        PersonFaceData person = new PersonFaceData();
        person.setPersonId(String.format("PSN_%010d", personIndex));

        // 设置分组
        if (groups != null && !groups.isEmpty()) {
            person.setGroupIds(groups.toArray(new String[0]));
        } else {
            person.setGroupIds(new String[]{"DEFAULT_GROUP"});
        }

        // 生成1-3张人脸模板
        int templateCount = MIN_TEMPLATES_PER_PERSON + random.nextInt(MAX_TEMPLATES_PER_PERSON - MIN_TEMPLATES_PER_PERSON + 1);
        List<CachedFaceFeature> features = new ArrayList<>(templateCount);

        // 预生成特征数据，减少对象创建
        byte[] featureData = new byte[FEATURE_DATA_SIZE];
        
        for (int t = 0; t < templateCount; t++) {
            CachedFaceFeature feature = new CachedFaceFeature();
            feature.setFaceId(String.format("FACE_%010d_%02d", personIndex, t));
            feature.setAlgoType("FACE310");
            feature.setTemplateType("NORMAL");

            // 生成模拟特征数据（每个模板使用不同的数据）
            random.nextBytes(featureData);
            // 复制数组以避免共享引用
            feature.setFeatureData(featureData.clone());

            features.add(feature);
        }

        person.setFeatures(features);
        return person;
    }

    /**
     * 将批量数据加载到缓存（优化版）
     */
    private void loadBatchToCache(List<PersonFaceData> batch, int threadIndex) {
        try {
            faceCacheService.loadFeatures(batch);
            long loaded = loadedPersons.addAndGet(batch.size());

            // 减少日志输出频率：每50万人输出一次
            if (loaded % 500000 < BATCH_SIZE) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                double percent = (loaded * 100.0) / totalPersonsTarget;
                long rate = elapsed > 0 ? loaded / elapsed : loaded;
                log.info("线程 {} | 进度: {}/{} ({}%) | 速率: {} 人/秒 | 已用时: {}s",
                        threadIndex, loaded, totalPersonsTarget, String.format("%.2f", percent), rate, elapsed);
            }
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
