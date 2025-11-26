package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.PersonFaceData;
import com.biometric.algo.service.FaceCacheService;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.entity.GrpPsn;
import com.biometric.serv.entity.PsnTmpl;
import com.biometric.serv.mapper.FaceFturMapper;
import com.biometric.serv.mapper.GrpPsnMapper;
import com.biometric.serv.mapper.PsnTmplMapper;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 优化后的数据加载服务
 */
@Service
public class DataLoadService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DataLoadService.class);

    private static final int BATCH_SIZE = 2000;
    private static final int LOG_INTERVAL = 20000;
    private static final String DEFAULT_GROUP_ID = "DEFAULT_GROUP";

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

    // --- 线程池配置 ---
    private final ThreadPoolExecutor loaderExecutor;

    public DataLoadService() {
        int cores = Runtime.getRuntime().availableProcessors();
        // 核心线程数：CPU核心数，最大线程数：核心数*2
        // 使用有界队列(100)防止积压过多任务导致OOM
        // CallerRunsPolicy：队列满时由主线程执行，实现自动背压(Backpressure)
        this.loaderExecutor = new ThreadPoolExecutor(
                cores, cores * 2,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactory() {
                    private final AtomicLong count = new AtomicLong(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "FaceLoader-" + count.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 加载数据的主入口
     */
    public void loadAllFeaturesIntoCache(int shardIndex, int totalShards) {
        log.info("Starting Parallel Data Load for shard {}/{} [Threads: {}]...",
                shardIndex, totalShards, loaderExecutor.getCorePoolSize());
        long startTime = System.currentTimeMillis();

        AtomicLong totalPersonsLoaded = new AtomicLong(0);
        List<String> psnIdBatch = new ArrayList<>(BATCH_SIZE);

        // Phaser用于同步，确保所有异步任务执行完毕
        // 注册1个party代表主线程
        Phaser phaser = new Phaser(1);

        try {
            // MyBatis 流式查询处理
            ResultHandler<PsnTmpl> handler = resultContext -> {
                PsnTmpl psn = resultContext.getResultObject();
                if (psn == null || psn.getPsnTmplNo() == null) return;

                psnIdBatch.add(psn.getPsnTmplNo());

                if (psnIdBatch.size() >= BATCH_SIZE) {
                    // 提交一批任务
                    submitBatchTask(new ArrayList<>(psnIdBatch), shardIndex, totalPersonsLoaded, phaser);
                    psnIdBatch.clear();
                }
            };

            // 1. 开始流式扫描数据库 (生产者)
            // 注意：Mapper XML中应配置 fetchSize="-2147483648" 以启用MySQL流式读取
            psnTmplMapper.streamScanPsnTmpls(shardIndex, totalShards, handler);

            // 2. 处理剩余的最后一批
            if (!psnIdBatch.isEmpty()) {
                submitBatchTask(new ArrayList<>(psnIdBatch), shardIndex, totalPersonsLoaded, phaser);
                psnIdBatch.clear();
            }

            // 3. 等待所有异步任务完成
            log.info("Database scan finished. Waiting for worker threads to complete...");
            phaser.arriveAndAwaitAdvance();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Shard {} load FINISHED. Total persons loaded: {}. Time elapsed: {}s",
                    shardIndex, totalPersonsLoaded.get(), duration);

        } catch (Exception e) {
            log.error("Fatal error loading shard " + shardIndex, e);
            throw new RuntimeException("Feature loading failed", e);
        }
    }

    /**
     * 提交批次处理任务到线程池
     */
    private void submitBatchTask(List<String> batchIds, int shardIndex, AtomicLong totalCounter, Phaser phaser) {
        // 注册一个新的任务
        phaser.register();

        CompletableFuture.runAsync(() -> {
            try {
                processBatch(batchIds, shardIndex, totalCounter);
            } catch (Exception e) {
                log.error("Error processing batch of size {}", batchIds.size(), e);
            } finally {
                // 任务完成，注销
                phaser.arriveAndDeregister();
            }
        }, loaderExecutor);
    }

    /**
     * 具体的批次处理逻辑 (消费者逻辑)
     */
    private void processBatch(List<String> psnIds, int shardIndex, AtomicLong totalCounter) {
        if (psnIds.isEmpty()) return;

        // 1. 批量获取组信息 (数据库IO)
        List<GrpPsn> groups = grpPsnMapper.selectByPsnIds(psnIds);
        Map<String, Set<String>> psnToGroups = new HashMap<>();
        for (GrpPsn g : groups) {
            if (g.getGrpId() != null) {
                psnToGroups.computeIfAbsent(g.getPsnTmplNo(), k -> new HashSet<>()).add(g.getGrpId());
            }
        }

        // 2. 批量获取特征 (数据库IO)
        List<FaceFtur> features = faceFturMapper.selectByPsnIds(psnIds);
        Map<String, List<CachedFaceFeature>> psnToFeatures = new HashMap<>();

        // 3. 特征转换与预计算 (CPU密集型 - 这一步并行化收益最大)
        for (FaceFtur f : features) {
            byte[] rawData = f.getFaceFturData();
            if (f.getFaceBosgId() != null && rawData != null && rawData.length > 0) {
                CachedFaceFeature cachedFeature = new CachedFaceFeature();
                cachedFeature.setFaceId(f.getFaceBosgId());
                cachedFeature.setTemplateType(f.getFaceCrteTmplType());
                cachedFeature.setAlgoType(f.getAlgoVerId());
                if(f.getAlgoVerId().toUpperCase().contains("NX")){
                    cachedFeature.setFeatureData(rawData);
                }

                try {
                    // 核心优化：并行计算二进制特征和浮点向量
                    if(minFeat){
                        int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(rawData);
                        cachedFeature.setBinaryFeature(binaryFeat);
                    }

                    if(maxFeat){
                        float[] floatFeat = Face303JavaCalcuater.toFloatArray(rawData);
                        cachedFeature.setFeatureVector(floatFeat);
                    }

                    psnToFeatures.computeIfAbsent(f.getPsnTmplNo(), k -> new ArrayList<>()).add(cachedFeature);
                } catch (Exception e) {
                    log.warn("Feature conversion failed for face: {}", f.getFaceBosgId());
                }
            }
        }

        // 4. 构建最终缓存对象
        List<PersonFaceData> personDataList = new ArrayList<>(psnIds.size());
        for (String psnId : psnIds) {
            List<CachedFaceFeature> featList = psnToFeatures.get(psnId);
            if (featList == null || featList.isEmpty()) {
                continue;
            }

            Set<String> grpSet = psnToGroups.get(psnId);
            if ((grpSet == null || grpSet.isEmpty()) && !allPerson) {
                continue;
            }

            if (grpSet == null) {
                grpSet = new HashSet<>();
                grpSet.add(DEFAULT_GROUP_ID);
            }else if (grpSet.isEmpty()) {
                grpSet.add(DEFAULT_GROUP_ID);
            }

            PersonFaceData data = new PersonFaceData();
            data.setPersonId(psnId);
            data.setFeatures(featList);
            data.setGroupIds(grpSet.toArray(new String[0]));

            personDataList.add(data);
        }

        // 5. 写入 Hazelcast 缓存 (网络IO)
        if (!personDataList.isEmpty()) {
            faceCacheService.loadFeatures(personDataList);

            long currentTotal = totalCounter.addAndGet(personDataList.size());
            if (currentTotal % LOG_INTERVAL < BATCH_SIZE) {
                log.info("Shard {}: Loaded {} persons...", shardIndex, currentTotal);
            }
        }
    }

    @Override
    public void destroy() {
        if (loaderExecutor != null) {
            log.info("Shutting down data loader thread pool...");
            loaderExecutor.shutdown();
        }
    }

}