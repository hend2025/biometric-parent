package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.PersonFaceData;
import com.biometric.algo.service.FaceCacheService;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.biometric.serv.config.ServerConfigOptimizer;
import com.biometric.serv.config.ServerConfigOptimizer.LoaderConfig;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.entity.GrpPsn;
import com.biometric.serv.entity.PsnTmpl;
import com.biometric.serv.mapper.FaceFturMapper;
import com.biometric.serv.mapper.GrpPsnMapper;
import com.biometric.serv.mapper.PsnTmplMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据加载服务，引入信号量实现内存保护
 */
@Slf4j
@Service
public class DataLoadService implements DisposableBean {

    private final int BATCH_SIZE;
    private final int LOG_INTERVAL;

    @Value("${biometric.face-loader.minFeat:true}")
    private boolean minFeat;

    @Value("${biometric.face-loader.maxFeat:true}")
    private boolean maxFeat;

    @Value("${biometric.face-loader.allPerson:false}")
    private boolean allPerson;

    @Autowired
    private PsnTmplMapper psnTmplMapper;
    @Autowired
    private GrpPsnMapper grpPsnMapper;
    @Autowired
    private FaceFturMapper faceFturMapper;
    @Autowired
    private FaceCacheService faceCacheService;

    private final ServerConfigOptimizer configOptimizer;

    private final ThreadPoolExecutor loaderExecutor;
    // 使用信号量控制最大并行批次，防止内存溢出。假设每个批次占用 10MB，允许 100 个批次并行约 1GB
    private final Semaphore memoryBackpressure;

    @Autowired
    public DataLoadService(ServerConfigOptimizer configOptimizer) {
        LoaderConfig config = configOptimizer.getLoaderConfig();
        this.configOptimizer = configOptimizer;
        this.BATCH_SIZE = config.batchSize;
        this.LOG_INTERVAL = config.logInterval;

        // 根据队列大小和线程数设置信号量
        int maxInFlightBatches = config.queueSize + config.maxThreads;
        this.memoryBackpressure = new Semaphore(maxInFlightBatches);

        this.loaderExecutor = new ThreadPoolExecutor(
                config.coreThreads,
                config.maxThreads,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(config.queueSize),
                new ThreadFactory() {
                    private final AtomicLong count = new AtomicLong(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "FaceLoader-" + count.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                // 队列满时由调用者运行，实现天然的背压
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void loadAllFeaturesIntoCache(int shardIndex, int totalShards) {
        log.info("开始分片 {}/{} 数据加载，批次大小: {}", shardIndex, totalShards, BATCH_SIZE);
        long startTime = System.currentTimeMillis();
        AtomicLong totalPersonsLoaded = new AtomicLong(0);
        List<String> psnIdBatch = new ArrayList<>(BATCH_SIZE);
        Phaser phaser = new Phaser(1);

        try {
            ResultHandler<PsnTmpl> handler = resultContext -> {
                PsnTmpl psn = resultContext.getResultObject();
                if (psn != null && psn.getPsnTmplNo() != null) {
                    psnIdBatch.add(psn.getPsnTmplNo());
                    if (psnIdBatch.size() >= BATCH_SIZE) {
                        // 提交任务，这里会阻塞直到有信号量许可
                        submitBatchTask(new ArrayList<>(psnIdBatch), shardIndex, totalPersonsLoaded, phaser);
                        psnIdBatch.clear();
                    }
                }
            };

            psnTmplMapper.streamScanPsnTmpls(shardIndex, totalShards, handler);

            if (!psnIdBatch.isEmpty()) {
                submitBatchTask(new ArrayList<>(psnIdBatch), shardIndex, totalPersonsLoaded, phaser);
            }

            // 等待所有任务完成
            phaser.arriveAndAwaitAdvance();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("分片 {} 加载完成！总人数: {}, 耗时: {}s", shardIndex, totalPersonsLoaded.get(), duration);

        } catch (Exception e) {
            log.error("加载异常", e);
            throw new RuntimeException("加载失败", e);
        }
    }

    private void submitBatchTask(List<String> batchIds, int shardIndex, AtomicLong totalCounter, Phaser phaser) {
        try {

            // 内存压力检测：如果内存使用过高，暂停提交新任务
            ServerConfigOptimizer.MemoryStats stats = configOptimizer.getCurrentMemoryStats();
            if (stats.usagePercent > 85) {
                log.error("内存使用率严重过高: [{}]，暂停加载数据到缓存!!!", stats);
                return;
            }

            // 获取许可，如果处理过慢会阻塞主线程，从而降低数据库读取速度
            memoryBackpressure.acquire();
            phaser.register();

            CompletableFuture.runAsync(() -> {
                try {
                    processBatch(batchIds, shardIndex, totalCounter);
                } finally {
                    memoryBackpressure.release();
                    phaser.arriveAndDeregister();
                }
            }, loaderExecutor);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("加载被中断", e);
        }
    }

    private void processBatch(List<String> psnIds, int shardIndex, AtomicLong totalCounter) {
        if (psnIds.isEmpty()) return;

        try {
            // 1. 批量获取组 (IO)
            List<GrpPsn> groups = grpPsnMapper.selectByPsnIds(psnIds);
            Map<String, Set<String>> psnToGroups = new HashMap<>();
            for (GrpPsn g : groups) {
                if (g.getGrpId() != null) {
                    psnToGroups.computeIfAbsent(g.getPsnTmplNo(), k -> new HashSet<>()).add(g.getGrpId());
                }
            }

            // 2. 批量获取特征 (IO)
            List<FaceFtur> features = faceFturMapper.selectByPsnIds(psnIds);
            Map<String, List<CachedFaceFeature>> psnToFeatures = new HashMap<>();

            // 3. 特征转换 (CPU)
            for (FaceFtur f : features) {
                byte[] rawData = f.getFaceFturData();
                if (f.getFaceBosgId() != null && rawData != null && rawData.length > 0) {
                    CachedFaceFeature cf = new CachedFaceFeature();
                    cf.setFaceId(f.getFaceBosgId());
                    cf.setTemplateType(f.getFaceCrteTmplType());
                    cf.setAlgoType(f.getAlgoVerId());
                    if(f.getAlgoVerId().toUpperCase().contains("NX")) {
                        cf.setFeatureData(rawData);
                    }

                    if (minFeat) cf.setBinaryFeature(Face303JavaCalcuater.getBinaFeat(rawData));
                    if (maxFeat) cf.setFeatureVector(Face303JavaCalcuater.toFloatArray(rawData));

                    psnToFeatures.computeIfAbsent(f.getPsnTmplNo(), k -> new ArrayList<>()).add(cf);
                }
            }

            // 4. 组装
            List<PersonFaceData> resultList = new ArrayList<>(psnIds.size());
            for (String pid : psnIds) {
                List<CachedFaceFeature> feats = psnToFeatures.get(pid);
                if (feats != null && !feats.isEmpty()) {
                    Set<String> grp = psnToGroups.get(pid);
                    if ((feats != null && !feats.isEmpty()) || allPerson) {
                        PersonFaceData data = new PersonFaceData();
                        data.setPersonId(pid);
                        data.setFeatures(feats);
                        data.setGroupIds(grp != null ? grp.toArray(new String[0]) : new String[]{"DEFAULT_GROUP"});
                        resultList.add(data);
                    }
                }
            }

            // 5. 写入缓存 (IO)
            if (!resultList.isEmpty()) {
                faceCacheService.loadFeatures(resultList);
                long current = totalCounter.addAndGet(resultList.size());
                if (current % LOG_INTERVAL < BATCH_SIZE) {
                    ServerConfigOptimizer.MemoryStats currentMemory = configOptimizer.getCurrentMemoryStats();
                    log.info("分片 {}: 已加载 {} 人... [{}]", shardIndex, current, currentMemory);
                }
            }

        } catch (Exception e) {
            log.error("批次处理失败", e);
        }
    }

    @Override
    public void destroy() {
        if (loaderExecutor != null) {
            loaderExecutor.shutdownNow();
        }
    }

}