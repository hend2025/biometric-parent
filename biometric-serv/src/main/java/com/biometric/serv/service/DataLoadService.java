package com.biometric.serv.service;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.service.FaceCacheService;
import com.biometric.serv.entity.GrpPsn;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.mapper.GrpPsnMapper;
import com.biometric.serv.mapper.FaceFturMapper;
import org.apache.ibatis.session.ResultContext;
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
        faceCacheService.clearCache();

        log.info("Step 1: Pre-loading Person-to-Group mappings into Hazelcast (chunked streaming)...");
        Map<String, Set<String>> psnToGroupMap = loadPsnToGroupMapDirectly();
        log.info("Step 1: Loaded {} unique person-group mappings into temporary cache.", psnToGroupMap.size());

        log.info("Step 2: Stream loading face features from DB with direct cache insertion...");
        AtomicLong totalFeaturesLoaded = new AtomicLong(0);
        Map<String, CachedFaceFeature> batchMap = new HashMap<>(BATCH_SIZE);

        ResultHandler<FaceFtur> handler = new ResultHandler<FaceFtur>() {
            @Override
            public void handleResult(ResultContext<? extends FaceFtur> resultContext) {
                FaceFtur feature = resultContext.getResultObject();
                
                Set<String> groupIds = psnToGroupMap.get(feature.getPsnTmplNo());
                if (groupIds == null) {
                    groupIds = Collections.emptySet();
                }
                
                CachedFaceFeature cachedFeature = new CachedFaceFeature(
                        feature.getFaceBosgId(),
                        feature.getPsnTmplNo(),
                        feature.getFaceFturData(),
                        groupIds
                );
                batchMap.put(cachedFeature.getFaceId(), cachedFeature);

                if (batchMap.size() >= BATCH_SIZE) {
                    faceCacheService.getFaceFeatureMap().putAll(batchMap);
                    long total = totalFeaturesLoaded.addAndGet(batchMap.size());
                    
                    if (total % LOG_INTERVAL == 0) {
                        log.info("Progress: {} features loaded...", total);
                    }
                    batchMap.clear();
                }
            }
        };

        faceFturMapper.streamScanAllFeatures(handler);

        if (!batchMap.isEmpty()) {
            faceCacheService.getFaceFeatureMap().putAll(batchMap);
            totalFeaturesLoaded.addAndGet(batchMap.size());
            log.info("Loaded last batch of {} features... (Total: {})",
                    batchMap.size(), totalFeaturesLoaded.get());
            batchMap.clear();
        }

        log.info("Step 3: Cleaning up temporary group mapping cache...");
        psnToGroupMap.clear();

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        log.info("Data load finished. Total {} features loaded in {} seconds.",
                totalFeaturesLoaded.get(), duration);
    }

    private Map<String, Set<String>> loadPsnToGroupMapDirectly() {
        Map<String, Set<String>> psnToGroupMap = new HashMap<>();

        final AtomicLong relationCount = new AtomicLong(0);
        final Map<String, Set<String>> chunkBuffer = new ConcurrentHashMap<>(GROUP_MAP_CHUNK_SIZE);
        
        ResultHandler<GrpPsn> handler = new ResultHandler<GrpPsn>() {
            @Override
            public void handleResult(ResultContext<? extends GrpPsn> resultContext) {
                GrpPsn relation = resultContext.getResultObject();
                
                chunkBuffer.computeIfAbsent(relation.getPsnTmplNo(), k -> ConcurrentHashMap.newKeySet())
                           .add(relation.getGrpId());
                
                long count = relationCount.incrementAndGet();
                
                if (chunkBuffer.size() >= GROUP_MAP_CHUNK_SIZE) {
                    psnToGroupMap.putAll(chunkBuffer);
                    log.info("... flushed {} persons to cache (total relations: {})",
                            chunkBuffer.size(), count);
                    chunkBuffer.clear();
                }

            }
        };
        
        GrpPsnMapper.streamScanAllRelations(handler);
        
        if (!chunkBuffer.isEmpty()) {
            psnToGroupMap.putAll(chunkBuffer);
            log.info("... flushed final {} persons to cache", chunkBuffer.size());
            chunkBuffer.clear();
        }
        
        log.info("Finished loading Person-to-Group mappings. Total relations: {}, Unique persons: {}",
                relationCount.get(), psnToGroupMap.size());
        return psnToGroupMap;
    }

}