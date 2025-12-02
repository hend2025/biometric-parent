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

    private static final int BATCH_SIZE = 1000;
    private static final int LOG_INTERVAL = 50000;
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
        // 核心线程数：减半避免过多并发，最大线程数：核心数
        // 使用有界队列(100)防止积压过多任务导致OOM
        // CallerRunsPolicy：队列满时由主线程执行，实现自动背压
        this.loaderExecutor = new ThreadPoolExecutor(
                cores, cores*2,
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
     * 
     * <p><b>内存占用计算（1000万人员，每人5组，2张模板）：</b></p>
     * <pre>
     * 1. CachedFaceFeature对象（每张模板）：
     *    - faceId (String ~32字符)               : ~104 字节
     *    - featureData (byte[512])               : 512 + 16(对象头) = 528 字节
     *    - templateType (String ~8字符)          : ~32 字节
     *    - algoType (String ~16字符)             : ~48 字节
     *    - binaryFeature (int[4])                : 16 + 16(对象头) = 32 字节
     *    - featureVector (float[128])            : 512 + 16(对象头) = 528 字节
     *    - 对象头                                 : 16 字节
     *    小计：1,288 字节 ≈ 1.26 KB/模板
     * 
     * 2. PersonFaceData对象（每人）：
     *    - personId (String ~32字符)             : ~104 字节
     *    - groupIds (String[5])                  : 5组×48 + 16(对象头) = 256 字节
     *    - features (ArrayList容器)              : 40 + 16(内部数组) = 56 字节
     *    - 对象头                                 : 16 字节
     *    小计：432 字节
     * 
     * 3. 每人总内存：
     *    = 432 (PersonFaceData基础) + 2 × 1,288 (2个模板)
     *    = 3,008 字节 ≈ 2.94 KB/人
     * 
     * 4. 1000万人总内存：
     *    = 10,000,000 × 3,008 = 30,080,000,000 字节
     *    = 30.08 GB (纯数据)
     *    ≈ 36.1 GB (考虑对象对齐+HashMap开销 ×1.2)
     * 
     * 建议：
     * - JVM堆内存：-Xmx48g -Xms48g（预留12GB缓冲）
     * - G1GC推荐配置：-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=32m
     * </pre>
     */
    public void loadAllFeaturesIntoCache(int shardIndex, int totalShards) {
        log.info("开始为分片 {}/{} 并行加载数据 [线程数: {}]...", shardIndex, totalShards, loaderExecutor.getCorePoolSize());
        long startTime = System.currentTimeMillis();

        AtomicLong totalPersonsLoaded = new AtomicLong(0);
        List<String> psnIdBatch = new ArrayList<>(BATCH_SIZE);

        // Phaser用于同步，确保所有异步任务执行完毕
        // 注册1个party代表主线程
        Phaser phaser = new Phaser(1);

        try {
            // MyBatis流式查询处理
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
            log.info("数据库扫描完成，等待工作线程完成...");
            phaser.arriveAndAwaitAdvance();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("分片 {} 加载完成。加载总人数: {}。耗时: {}秒", shardIndex, totalPersonsLoaded.get(), duration);

        } catch (Exception e) {
            log.error("加载分片 " + shardIndex + " 时发生严重错误", e);
            throw new RuntimeException("特征加载失败", e);
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
                log.error("处理大小为 {} 的批次时出错", batchIds.size(), e);
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
        Map<String, Set<String>> psnToGroups = new HashMap<>((int)(psnIds.size() / 0.75) + 1);
        for (GrpPsn g : groups) {
            if (g.getGrpId() != null) {
                psnToGroups.computeIfAbsent(g.getPsnTmplNo(), k -> new HashSet<>()).add(g.getGrpId());
            }
        }
        groups.clear();
        groups = null;

        // 2. 批量获取特征 (数据库IO)
        List<FaceFtur> features = faceFturMapper.selectByPsnIds(psnIds);
        Map<String, List<CachedFaceFeature>> psnToFeatures = new HashMap<>((int)(psnIds.size() / 0.75) + 1);

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
                    log.warn("人脸特征转换失败: {}", f.getFaceBosgId());
                }
            }
        }
        // 及时释放特征列表内存
        features.clear();
        features = null;

        // 4. 构建最终缓存对象
        List<PersonFaceData> personDataList = new ArrayList<>((int)(psnIds.size() / 0.75) + 1);
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
                log.info("分片 {}: 已加载 {} 人...", shardIndex, currentTotal);
            }
        }

        
        // 清理临时数据结构释放内存
        personDataList.clear();
        psnToFeatures.clear();
        psnToGroups.clear();
        personDataList = null;
        psnToFeatures = null;
        psnToGroups = null;

    }

    @Override
    public void destroy() {
        if (loaderExecutor != null) {
            log.info("正在关闭数据加载线程池...");
            loaderExecutor.shutdown();
        }
    }

}