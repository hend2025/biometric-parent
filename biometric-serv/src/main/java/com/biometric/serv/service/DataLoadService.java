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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DataLoadService {

    private static final Logger log = LoggerFactory.getLogger(DataLoadService.class);
    private static final int BATCH_SIZE = 2000;
    private static final int LOG_INTERVAL = 10000;
    private static final String DEFAULT_GROUP_ID = "DEFAULT_GROUP";

    @Value("${biometric.face-loader.maxFeat:false}")
    private boolean maxFeat;

    @Value("${biometric.face-loader.minFeat:false}")
    private boolean minFeat;

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

    public void loadAllFeaturesIntoCache(int shardIndex, int totalShards) {
        log.info("Starting optimized data load for shard {}/{}...", shardIndex, totalShards);
        long startTime = System.currentTimeMillis();

        AtomicLong totalPersonsLoaded = new AtomicLong(0);
        List<String> psnIdBatch = new ArrayList<>(BATCH_SIZE);

        ResultHandler<PsnTmpl> handler = resultContext -> {
            PsnTmpl psn = resultContext.getResultObject();
            if (psn == null || psn.getPsnTmplNo() == null) return;

            psnIdBatch.add(psn.getPsnTmplNo());

            if (psnIdBatch.size() >= BATCH_SIZE) {
                processBatch(psnIdBatch, shardIndex, totalPersonsLoaded);
                psnIdBatch.clear();
            }
        };

        try {
            // 注意：流式查询通常依赖底层驱动的游标，确保Mapper配置正确 (fetchSize)
            psnTmplMapper.streamScanPsnTmpls(shardIndex, totalShards, handler);

            if (!psnIdBatch.isEmpty()) {
                processBatch(psnIdBatch, shardIndex, totalPersonsLoaded);
                psnIdBatch.clear();
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Shard {} load finished. Total persons: {}. Time: {}s",
                    shardIndex, totalPersonsLoaded.get(), duration);

        } catch (Exception e) {
            log.error("Fatal error loading shard " + shardIndex, e);
            throw new RuntimeException("Feature loading failed", e);
        }
    }

    private void processBatch(List<String> psnIds, int shardIndex, AtomicLong totalCounter) {
        if (psnIds.isEmpty()) return;

        // 1. 批量获取组信息
        List<GrpPsn> groups = grpPsnMapper.selectByPsnIds(psnIds);
        Map<String, Set<String>> psnToGroups = new HashMap<>();
        for (GrpPsn g : groups) {
            if (g.getGrpId() != null) {
                psnToGroups.computeIfAbsent(g.getPsnTmplNo(), k -> new HashSet<>()).add(g.getGrpId());
            }
        }

        // 2. 批量获取特征并**预计算**
        List<FaceFtur> features = faceFturMapper.selectByPsnIds(psnIds);
        Map<String, List<CachedFaceFeature>> psnToFeatures = new HashMap<>();

        for (FaceFtur f : features) {
            byte[] rawData = f.getFaceFturData();
            if (f.getFaceBosgId() != null && rawData != null && rawData.length > 0) {
                CachedFaceFeature cachedFeature = new CachedFaceFeature();
                cachedFeature.setFaceId(f.getFaceBosgId());
                cachedFeature.setFeatureData(rawData);
                cachedFeature.setTemplateType(f.getFaceCrteTmplType());
                cachedFeature.setAlgoType(f.getAlgoVerId());

                try {
                    if (minFeat){
                        // 转换二进制特征 (int[])
                        int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(rawData);
                        cachedFeature.setBinaryFeature(binaryFeat);
                    }
                    if(maxFeat){
                        // 转换浮点向量 (float[])
                        float[] floatFeat = Face303JavaCalcuater.toFloatArray(rawData);
                        cachedFeature.setFeatureVector(floatFeat);
                    }

                    psnToFeatures.computeIfAbsent(f.getPsnTmplNo(), k -> new ArrayList<>()).add(cachedFeature);

                } catch (Exception e) {
                    log.warn("Feature conversion failed for face: {}", f.getFaceBosgId());
                }
            }
        }

        // 3. 构建 PersonFaceData
        List<PersonFaceData> personDataList = new ArrayList<>(psnIds.size());
        for (String psnId : psnIds) {
            List<CachedFaceFeature> featList = psnToFeatures.get(psnId);
            if (featList == null || featList.isEmpty()) {
                continue; // 没有有效人脸特征的人员不加载
            }

            Set<String> grpSet = psnToGroups.get(psnId);
            if((grpSet == null || grpSet.isEmpty()) && allPerson== false) {
                continue; // 没有组信息的人员不加载
            }
            // 如果人员没有组，赋予默认组
            if (grpSet == null) {
                grpSet = new HashSet<>();
                grpSet.add(DEFAULT_GROUP_ID);
            } else if (grpSet.isEmpty()) {
                grpSet.add(DEFAULT_GROUP_ID);
            }

            PersonFaceData data = new PersonFaceData();
            data.setPersonId(psnId);
            data.setFeatures(featList);
            data.setGroupIds(grpSet.toArray(new String[0]));

            personDataList.add(data);
        }

        // 4. 写入缓存
        if (!personDataList.isEmpty()) {
            faceCacheService.loadFeatures(personDataList);
            long total = totalCounter.addAndGet(personDataList.size());
            if (total % LOG_INTERVAL < BATCH_SIZE) {
                log.info("Shard {}: Loaded {} persons...", shardIndex, total);
            }
        }
    }

}