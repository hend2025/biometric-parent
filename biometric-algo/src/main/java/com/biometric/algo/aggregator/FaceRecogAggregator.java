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
 * 优化后的 Hazelcast 分布式聚合器
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
    }

    // 初始化输入特征（搜索查询特征）
    private void initInputFeatures() {
        if (inputFloatFeatures != null) return;

        inputFloatFeatures = new ArrayList<>();
        inputBinaryFeatures = new ArrayList<>();
        List<String> faceIdList = new ArrayList<>();

        if (compareParams != null && !CollectionUtils.isEmpty(compareParams.getFeatures())) {
            int idx = 0;
            for (byte[] feature : compareParams.getFeatures()) {
                if (feature == null || feature.length == 0) {
                    idx++;
                    continue;
                }
                // 输入特征只计算一次，此处开销可忽略
                int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(feature);
                float[] floatFeat = Face303JavaCalcuater.toFloatArray(feature);

                if (binaryFeat != null && floatFeat != null) {
                    inputBinaryFeatures.add(binaryFeat);
                    inputFloatFeatures.add(floatFeat);
                    faceIdList.add(String.valueOf(idx));
                }
                idx++;
            }
            inputFaceIdStrings = faceIdList.toArray(new String[0]);
        }
    }

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

        // 懒加载初始化输入特征
        if (inputBinaryFeatures == null) {
            initInputFeatures();
        }
        if (inputBinaryFeatures.isEmpty()) return;

        PersonFaceData personData = entry.getValue();
        List<CachedFaceFeature> features = personData.getFeatures();
        if (features == null || features.isEmpty()) return;

        float maxPersonScore = -1.0f;
        String maxPersonFaceId = null;

        // 用于收集通过阈值的详细匹配结果（懒初始化）
        List<CompareResult.compareDetails> matchedDetails = null;

        int inputSize = inputBinaryFeatures.size();

        for (CachedFaceFeature candidate : features) {
            // 1. 直接获取预计算特征
            int[] candidateBinaryFeat = candidate.getBinaryFeature();
            if (candidateBinaryFeat == null) {
                candidateBinaryFeat = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());
                if (candidateBinaryFeat == null) continue;
            }

            float[] candidateFloatFeat = candidate.getFeatureVector();
            if (candidateFloatFeat == null) {
                candidateFloatFeat = Face303JavaCalcuater.toFloatArray(candidate.getFeatureData());
                if (candidateFloatFeat == null) continue;
            }

            // 2. 与所有输入特征比对
            for (int i = 0; i < inputSize; i++) {
                int[] inputBFeat = inputBinaryFeatures.get(i);

                // 2.1 汉明距离粗筛 (位运算，极快)
                boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                        inputBFeat[0], inputBFeat[1], inputBFeat[2], inputBFeat[3],
                        candidateBinaryFeat[0], candidateBinaryFeat[1], candidateBinaryFeat[2], candidateBinaryFeat[3],
                        HAMMING_DIST_THRESHOLD
                );

                if (isSimilar) {
                    // 2.2 余弦相似度精筛 (浮点运算)
                    float similarity = Face303JavaCalcuater.compare(inputFloatFeatures.get(i), candidateFloatFeat);

                    // 记录该人员的最佳分数
                    if (similarity > maxPersonScore) {
                        maxPersonScore = similarity;
                        maxPersonFaceId = candidate.getFaceId();
                    }

                    // 2.3 只有超过阈值，才创建详情对象
                    if (similarity >= compareParams.getThreshold()) {
                        if (matchedDetails == null) {
                            matchedDetails = new ArrayList<>();
                        }
                        CompareResult.compareDetails detail = new CompareResult.compareDetails();
                        detail.setFaceId1(inputFaceIdStrings[i]);
                        detail.setFaceId2(candidate.getFaceId());
                        detail.setScore(similarity);
                        detail.setMatched(true);
                        matchedDetails.add(detail);
                    }
                }
            }
        }

        // 3. 更新局部堆
        if (matchedDetails != null && !matchedDetails.isEmpty()) {
            CompareResult result = new CompareResult();
            result.setPsnTmplNo(personData.getPersonId());
            result.setScore(maxPersonScore);
            result.setFaceId(maxPersonFaceId);
            result.setMatched(true);
            result.setDetails(matchedDetails);

            updateHeap(result);
        }
    }

    private void updateHeap(CompareResult result) {
        if (localTopNHeap.size() < compareParams.getTopN()) {
            localTopNHeap.add(result);
        } else {
            CompareResult head = localTopNHeap.peek();
            if (head != null && result.getScore() > head.getScore()) {
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

        for (CompareResult result : other.localTopNHeap) {
            updateHeap(result);
        }
    }

    @Override
    public List<CompareResult> aggregate() {
        if (localTopNHeap == null) return Collections.emptyList();

        List<CompareResult> results = new ArrayList<>(localTopNHeap);
        // 按分数降序排列
        results.sort((r1, r2) -> Float.compare(r2.getScore(), r1.getScore()));

        // 补全 min/max 统计信息 (可选)
        for (CompareResult r : results) {
            fillMinMaxStats(r);
        }
        return results;
    }

    private void fillMinMaxStats(CompareResult result) {
        if (result.getDetails() == null) return;
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        String minId = null, maxId = null;
        for (CompareResult.compareDetails d : result.getDetails()) {
            if (d.getScore() < min) { min = d.getScore(); minId = d.getFaceId2(); }
            if (d.getScore() > max) { max = d.getScore(); maxId = d.getFaceId2(); }
        }
        result.setMaxScore(max != Float.MIN_VALUE ? max : result.getScore());
        result.setMaxFaceId(maxId != null ? maxId : result.getFaceId());
        result.setMinScore(min != Float.MAX_VALUE ? min : result.getScore());
        result.setMinFaceId(minId != null ? minId : result.getFaceId());
    }

}