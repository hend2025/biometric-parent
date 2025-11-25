package com.biometric.algo.aggregator;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.CompareParams;
import com.biometric.algo.dto.CompareResult;
import com.biometric.algo.dto.PersonFaceData;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.hazelcast.aggregation.Aggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Hazelcast 分布式聚合器 - 用于内存网格中的人脸 1:N 搜索
 */
@Slf4j
public class FaceRecogAggregator implements Aggregator<Map.Entry<String, PersonFaceData>, List<CompareResult>>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HAMMING_DIST_THRESHOLD = 50;

    private transient List<float[]> inputFloatFeatures;
    private transient List<int[]> inputBinaryFeatures;
    private transient String[] inputFaceIdStrings;

    private CompareParams compareParams;
    private PriorityQueue<CompareResult> localTopNHeap;

    public FaceRecogAggregator(CompareParams params) {
        this.compareParams = params;
        this.localTopNHeap = new PriorityQueue<>(params.getTopN(), new CompareResultScoreComparator());
        initInputFeatures();
    }

    private void initInputFeatures() {
        if (inputFloatFeatures != null && inputBinaryFeatures != null) {
            return;
        }
        inputFloatFeatures = new ArrayList<>();
        inputBinaryFeatures = new ArrayList<>();

        if (compareParams != null && !CollectionUtils.isEmpty(compareParams.getFeatures())) {
            int size = compareParams.getFeatures().size();
            inputFaceIdStrings = new String[size];
            int idx = 0;
            for (byte[] feature : compareParams.getFeatures()) {
                if (feature == null || feature.length == 0) {
                    inputFaceIdStrings[idx] = Integer.toString(idx);
                    idx++;
                    continue;
                }

                int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(feature);
                float[] floatFeat = Face303JavaCalcuater.toFloatArray(feature);

                if (binaryFeat != null && floatFeat != null) {
                    inputBinaryFeatures.add(binaryFeat);
                    inputFloatFeatures.add(floatFeat);
                    inputFaceIdStrings[idx] = Integer.toString(idx);
                }
                idx++;
            }
        }
    }

    // 自定义可序列化比较器类
    private static class CompareResultScoreComparator implements Comparator<CompareResult>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(CompareResult r1, CompareResult r2) {
            return Float.compare(r1.getScore(), r2.getScore());
        }
    }

    @Override
    public void accumulate(Map.Entry<String, PersonFaceData> entry) {
        if (entry == null || entry.getValue() == null) return;

        // 确保输入特征在当前节点已初始化
        if (inputBinaryFeatures == null) {
            initInputFeatures();
        }
        if (inputBinaryFeatures.isEmpty()) return;

        PersonFaceData personData = entry.getValue();
        List<CachedFaceFeature> features = personData.getFeatures();

        if (features == null || features.isEmpty()) return;

        float maxScore = -1.0f;
        String maxFaceId = null;
        boolean passedBinaryFilter = false;

        // 使用数组缓存比对详情，避免频繁创建对象
        int inputSize = inputBinaryFeatures.size();
        int featSize = features.size();
        int maxDetails = inputSize * featSize;
        
        float[] detailScores = new float[maxDetails];
        int[] detailInputIdx = new int[maxDetails];
        String[] detailCandidateFaceIds = new String[maxDetails];
        int detailCount = 0;

        for (CachedFaceFeature candidate : features) {
            // 1. 获取候选人二进制特征（优先使用缓存的）
            int[] candidateBinaryFeat = candidate.getBinaryFeature();
            if (candidateBinaryFeat == null) {
                candidateBinaryFeat = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());
            }
            if (candidateBinaryFeat == null) continue;

            float[] candidateFloatFeat = null;

            // 2. 遍历所有输入特征进行比对
            for (int i = 0; i < inputSize; i++) {
                int[] inputBFeat = inputBinaryFeatures.get(i);

                // 2.1 粗筛：汉明距离检查
                boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                        inputBFeat[0], inputBFeat[1], inputBFeat[2], inputBFeat[3],
                        candidateBinaryFeat[0], candidateBinaryFeat[1], candidateBinaryFeat[2], candidateBinaryFeat[3],
                        HAMMING_DIST_THRESHOLD
                );

                float similarity = 0f;
                if (isSimilar) {
                    // 2.2 精筛：余弦相似度计算
                    if (candidateFloatFeat == null) {
                        candidateFloatFeat = Face303JavaCalcuater.toFloatArray(candidate.getFeatureData());
                    }

                    similarity = Face303JavaCalcuater.compare(inputFloatFeatures.get(i), candidateFloatFeat);

                    // 记录最大相似度
                    if (similarity > maxScore) {
                        maxScore = similarity;
                        maxFaceId = candidate.getFaceId();
                        passedBinaryFilter = true;
                    }
                }

                // 暂存结果，不立即创建对象
                detailScores[detailCount] = similarity;
                detailInputIdx[detailCount] = i;
                detailCandidateFaceIds[detailCount] = candidate.getFaceId();
                detailCount++;
            }
        }

        // 3. 阈值判断与堆更新
        if (passedBinaryFilter && maxScore >= compareParams.getThreshold()) {
            // 只有在确认匹配时，才批量创建详情对象
            List<CompareResult.compareDetails> details = new ArrayList<>(detailCount);
            for (int i = 0; i < detailCount; i++) {
                CompareResult.compareDetails detail = new CompareResult.compareDetails();
                detail.setFaceId1(inputFaceIdStrings[detailInputIdx[i]]);
                detail.setFaceId2(detailCandidateFaceIds[i]);
                detail.setScore(detailScores[i]);
                detail.setMatched(detailScores[i] >= compareParams.getThreshold());
                details.add(detail);
            }

            CompareResult result = new CompareResult();
            result.setPsnTmplNo(personData.getPersonId());
            result.setScore(maxScore);
            result.setFaceId(maxFaceId);
            result.setMatched(true);
            result.setDetails(details);
            
            // 如果堆未满，直接添加
            if (localTopNHeap.size() < compareParams.getTopN()) {
                localTopNHeap.add(result);
            }
            // 如果堆满了，且当前分数高于堆顶（堆顶是最小的），则替换
            else if (localTopNHeap.peek() != null && maxScore > localTopNHeap.peek().getScore()) {
                localTopNHeap.poll();
                localTopNHeap.add(result);
            }
        }

    }

    @Override
    public void combine(Aggregator aggregator) {
        if (!(aggregator instanceof FaceRecogAggregator)) return;

        FaceRecogAggregator other = (FaceRecogAggregator) aggregator;
        if (other.localTopNHeap == null) return;

        // 合并其他分片的 TopN 结果
        for (CompareResult otherResult : other.localTopNHeap) {
            // 复用堆更新逻辑
            if (this.localTopNHeap.size() < compareParams.getTopN()) {
                this.localTopNHeap.add(otherResult);
            } else if (this.localTopNHeap.peek() != null && otherResult.getScore() > this.localTopNHeap.peek().getScore()) {
                this.localTopNHeap.poll();
                this.localTopNHeap.add(otherResult);
            }
        }
    }

    @Override
    public List<CompareResult> aggregate() {
        List<CompareResult> results = new ArrayList<>();
        if (localTopNHeap == null) return Collections.emptyList();
        for (CompareResult result : localTopNHeap) {
            float minScore = Float.MAX_VALUE;
            float maxScore = Float.MIN_VALUE;
            String minFaceId = null;
            String maxFaceId = null;
            for (CompareResult.compareDetails detail : result.getDetails()) {
                if (detail.getScore() < minScore) {
                    minScore = detail.getScore();
                    minFaceId = detail.getFaceId2();
                }
                if (detail.getScore() > maxScore) {
                    maxScore = detail.getScore();
                    maxFaceId = detail.getFaceId2();
                }
            }
            result.setMaxFaceId(maxFaceId != null ? maxFaceId : result.getFaceId());
            result.setMaxScore(maxScore != Float.MIN_VALUE ? maxScore : result.getScore());
            result.setMinFaceId(minFaceId != null ? minFaceId : result.getFaceId());
            result.setMinScore(minScore != Float.MAX_VALUE ? minScore : result.getScore());
            results.add(result);
        }
        // 最终输出时，按分数从高到低排序
        results.sort((r1, r2) -> Float.compare(r2.getScore(), r1.getScore()));
        return results;
    }

}