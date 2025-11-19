package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.service.FaceCacheService;
import com.biometric.serv.entity.GrpPsn;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.mapper.GrpPsnMapper;
import com.biometric.serv.mapper.FaceFturMapper;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DataLoadService {

    private static final Logger log = LoggerFactory.getLogger(DataLoadService.class);
    private static final int BATCH_SIZE = 1000;
    private static final int LOG_INTERVAL = 10000;
    private static final int GROUP_MAP_CHUNK_SIZE = 10000;

    @Autowired
    private GrpPsnMapper GrpPsnMapper;
    @Autowired
    private FaceFturMapper faceFturMapper;
    @Autowired
    private FaceCacheService faceCacheService;

    @Transactional(readOnly = true)
    public void loadAllFeaturesIntoCache() {
        log.info("Starting biometric data load into Hazelcast cache...");
        long startTime = System.currentTimeMillis();

        try {
            faceCacheService.clearCache();

            log.info("Step 1: Pre-loading Person-to-Group mappings into Hazelcast (chunked streaming)...");
            Map<String, Set<String>> psnToGroupMap = loadPsnToGroupMapDirectly();

            log.info("Step 1: Loaded {} unique person-group mappings into temporary cache.", psnToGroupMap.size());

            log.info("Step 2: Stream loading face features from DB with direct cache insertion...");
            loadFeaturesWithGroupMapping(psnToGroupMap);

            log.info("Step 3: Cleaning up temporary group mapping cache...");
            psnToGroupMap.clear();

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Data load finished successfully in {} seconds.", duration);

        } catch (Exception e) {
            log.error("Failed to load features into cache", e);
            throw new RuntimeException("Feature loading failed", e);
        }
    }

    private void loadFeaturesWithGroupMapping(Map<String, Set<String>> psnToGroupMap) {
        AtomicLong totalFeaturesLoaded = new AtomicLong(0);
        Map<String, CachedFaceFeature> batchMap = new HashMap<>(BATCH_SIZE);

        ResultHandler<FaceFtur> handler = resultContext -> {
            FaceFtur feature = resultContext.getResultObject();

            if (feature == null || feature.getFaceBosgId() == null ||
                    feature.getFaceFturData() == null || feature.getFaceFturData().length != 512) {
                log.warn("Skipping invalid feature record: {}",
                        feature != null ? feature.getFaceBosgId() : "null");
                return;
            }

            Set<String> groupIds = psnToGroupMap.getOrDefault(feature.getPsnTmplNo(), Collections.emptySet());

            CachedFaceFeature cachedFeature = new CachedFaceFeature(
                    feature.getFaceBosgId(),
                    feature.getPsnTmplNo(),
                    feature.getFaceFturData(),
                    groupIds
            );

            synchronized (batchMap) {
                batchMap.put(cachedFeature.getFaceId(), cachedFeature);

                if (batchMap.size() >= BATCH_SIZE) {
                    faceCacheService.getFaceFeatureMap().putAll(new HashMap<>(batchMap));
                    long total = totalFeaturesLoaded.addAndGet(batchMap.size());

                    if (total % LOG_INTERVAL == 0) {
                        log.info("Progress: {} features loaded...", total);
                    }
                    batchMap.clear();
                }
            }
        };

        try {
            faceFturMapper.streamScanAllFeatures(handler);

            synchronized (batchMap) {
                if (!batchMap.isEmpty()) {
                    faceCacheService.getFaceFeatureMap().putAll(batchMap);
                    totalFeaturesLoaded.addAndGet(batchMap.size());
                    log.info("Loaded last batch of {} features... (Total: {})",
                            batchMap.size(), totalFeaturesLoaded.get());
                    batchMap.clear();
                }
            }

            log.info("Total {} features loaded successfully.", totalFeaturesLoaded.get());

        } catch (Exception e) {
            log.error("Error during feature streaming", e);
            throw new RuntimeException("Failed to stream features from database", e);
        }
    }

    private Map<String, Set<String>> loadPsnToGroupMapDirectly() {
        Map<String, Set<String>> psnToGroupMap = new ConcurrentHashMap<>();

        AtomicLong relationCount = new AtomicLong(0);
        Map<String, Set<String>> chunkBuffer = new ConcurrentHashMap<>(GROUP_MAP_CHUNK_SIZE);

        ResultHandler<GrpPsn> handler = resultContext -> {
            GrpPsn relation = resultContext.getResultObject();

            if (relation == null || relation.getPsnTmplNo() == null || relation.getGrpId() == null) {
                log.warn("Skipping invalid group-person relation");
                return;
            }

            chunkBuffer.computeIfAbsent(relation.getPsnTmplNo(), k -> ConcurrentHashMap.newKeySet())
                    .add(relation.getGrpId());

            long count = relationCount.incrementAndGet();

            if (chunkBuffer.size() >= GROUP_MAP_CHUNK_SIZE) {
                synchronized (psnToGroupMap) {
                    for (Map.Entry<String, Set<String>> entry : chunkBuffer.entrySet()) {
                        String personId = entry.getKey();
                        Set<String> groups = entry.getValue();
                        psnToGroupMap.computeIfAbsent(personId, k -> ConcurrentHashMap.newKeySet()).addAll(groups);
                    }
                }
                log.info("... flushed {} persons to cache (total relations: {})", chunkBuffer.size(), count);
                chunkBuffer.clear();
            }
        };

        try {
            GrpPsnMapper.streamScanAllRelations(handler);

            if (!chunkBuffer.isEmpty()) {
                synchronized (psnToGroupMap) {
                    for (Map.Entry<String, Set<String>> entry : chunkBuffer.entrySet()) {
                        String personId = entry.getKey();
                        Set<String> groups = entry.getValue();
                        psnToGroupMap.computeIfAbsent(personId, k -> ConcurrentHashMap.newKeySet()).addAll(groups);
                    }
                }
                log.info("... flushed final {} persons to cache", chunkBuffer.size());
                chunkBuffer.clear();
            }

            log.info("Finished loading Person-to-Group mappings. Total relations: {}, Unique persons: {}",
                    relationCount.get(), psnToGroupMap.size());
            return psnToGroupMap;

        } catch (Exception e) {
            log.error("Error loading person-group mappings", e);
            throw new RuntimeException("Failed to load person-group mappings", e);
        }
    }

}