package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.PersonFaceData;
import com.biometric.algo.service.FaceCacheService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DataLoadService {

    private static final Logger log = LoggerFactory.getLogger(DataLoadService.class);
    private static final int BATCH_SIZE = 2000;
    private static final int LOG_INTERVAL = 10000;

    @Autowired
    private PsnTmplMapper psnTmplMapper;
    @Autowired
    private GrpPsnMapper grpPsnMapper;
    @Autowired
    private FaceFturMapper faceFturMapper;
    @Autowired
    private FaceCacheService faceCacheService;

    @Transactional(readOnly = true)
    public void loadAllFeaturesIntoCache(int shardIndex, int totalShards) {
        log.info("Starting biometric data load into Hazelcast cache for shard {}/{}...", shardIndex, totalShards);
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
            psnTmplMapper.streamScanPsnTmpls(shardIndex, totalShards, handler);

            if (!psnIdBatch.isEmpty()) {
                processBatch(psnIdBatch, shardIndex, totalPersonsLoaded);
                psnIdBatch.clear();
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Data load for shard {} finished successfully in {} seconds. Total persons: {}", 
                    shardIndex, duration, totalPersonsLoaded.get());

        } catch (Exception e) {
            log.error("Failed to load features into cache for shard " + shardIndex, e);
            throw new RuntimeException("Feature loading failed", e);
        }
    }

    private void processBatch(List<String> psnIds, int shardIndex, AtomicLong totalCounter) {
        if (psnIds.isEmpty()) return;

        // 1. Batch fetch groups
        List<GrpPsn> groups = grpPsnMapper.selectByPsnIds(psnIds);
        Map<String, Set<String>> psnToGroups = new HashMap<>();
        for (GrpPsn g : groups) {
            if (g.getGrpId() != null) {
                psnToGroups.computeIfAbsent(g.getPsnTmplNo(), k -> new HashSet<>()).add(g.getGrpId());
            }
        }

        // 2. Batch fetch features
        List<FaceFtur> features = faceFturMapper.selectByPsnIds(psnIds);
        Map<String, List<CachedFaceFeature>> psnToFeatures = new HashMap<>();
        for (FaceFtur f : features) {
            if (f.getFaceBosgId() != null && f.getFaceFturData() != null) {
                CachedFaceFeature cachedFeature = new CachedFaceFeature();
                cachedFeature.setFaceId(f.getFaceBosgId());
                cachedFeature.setFeatureData(f.getFaceFturData());
                cachedFeature.setTemplateType(f.getFaceCrteTmplType());
                cachedFeature.setAlgoType(f.getAlgoVerId());
                
                psnToFeatures.computeIfAbsent(f.getPsnTmplNo(), k -> new ArrayList<>()).add(cachedFeature);
            }
        }

        // 3. Build PersonFaceData objects
        List<PersonFaceData> personDataList = new ArrayList<>(psnIds.size());
        for (String psnId : psnIds) {
            List<CachedFaceFeature> featList = psnToFeatures.get(psnId);
            if (featList==null || featList.size()==0){
                continue;
            }
            Set<String> grpSet = psnToGroups.get(psnId);
            if (grpSet == null || grpSet.size()==0) {
                continue;
            }

            PersonFaceData data = new PersonFaceData();
            data.setPersonId(psnId);
            data.setFeatures(featList);
            data.setGroupIds(grpSet.toArray(new String[0]));
            
            personDataList.add(data);
        }

        // 4. Load to cache
        if (!personDataList.isEmpty()) {
            faceCacheService.loadFeatures(personDataList);
            long total = totalCounter.addAndGet(personDataList.size());
            if (total % LOG_INTERVAL < BATCH_SIZE) {
                log.info("Shard {}: Progress: {} persons loaded...", shardIndex, total);
            }
        }
    }

}