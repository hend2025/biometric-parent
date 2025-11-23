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

    private CompareParams CompareParams;
    private PriorityQueue<CompareResult> localTopNHeap;

    public FaceRecogAggregator(CompareParams params) {
        this.CompareParams = params;
        // 使用自定义可序列化比较器替代lambda表达式
        this.localTopNHeap = new PriorityQueue<>(params.getTopN(), new CompareResultScoreComparator());
        // 注意：构造函数是在调用端执行的，initInputFeatures 可能需要在 accumulate 首次调用时再次检查
        initInputFeatures();
    }

    private void initInputFeatures() {
        if (inputFloatFeatures != null && inputBinaryFeatures != null) {
            return;
        }
        inputFloatFeatures = new ArrayList<>();
        inputBinaryFeatures = new ArrayList<>();

        if (CompareParams != null && !CollectionUtils.isEmpty(CompareParams.getFeatures())) {
            for (byte[] feature : CompareParams.getFeatures()) {
                if (feature == null || feature.length == 0) continue;

                int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(feature);
                float[] floatFeat = Face303JavaCalcuater.toFloatArray(feature);

                if (binaryFeat != null && floatFeat != null) {
                    inputBinaryFeatures.add(binaryFeat);
                    inputFloatFeatures.add(floatFeat);
                }
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

        for (CachedFaceFeature candidate : features) {
            processCandidate(candidate, personData.getPersonId());
        }
    }

    private void processCandidate(CachedFaceFeature candidate, String psnTmplNo) {
        // 1. 获取候选人二进制特征（优先使用缓存的）
        int[] candidateBinaryFeat = candidate.getBinaryFeature();
        if (candidateBinaryFeat == null) {
            candidateBinaryFeat = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());
        }
        if (candidateBinaryFeat == null) return;

        float maxScore = -1.0f;
        float[] candidateFloatFeat = null;
        boolean passedBinaryFilter = false;

        // 2. 遍历所有输入特征进行比对（1:N 中的 N 对比输入的多张人脸）
        List<CompareResult.compareDetails> details = new ArrayList<>();
        for (int i = 0; i < inputBinaryFeatures.size(); i++) {
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
                    passedBinaryFilter = true;
                }
            }

            CompareResult.compareDetails detail = new CompareResult.compareDetails();
            detail.setFaceId1(Integer.toString(i));
            detail.setFaceId2(candidate.getFaceId());
            detail.setScore(similarity);
            detail.setMatched(similarity >= CompareParams.getThreshold());
            details.add(detail);

        }

        // 3. 阈值判断与堆更新
        if (passedBinaryFilter && maxScore >= CompareParams.getThreshold()) {
            CompareResult result = new CompareResult();
            result.setPsnTmplNo(psnTmplNo);
            result.setFaceId(candidate.getFaceId());
            result.setMatched(true);
            result.setScore(maxScore);
            result.setDetails(details);
            // 如果堆未满，直接添加
            if (localTopNHeap.size() < CompareParams.getTopN()) {
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
            if (this.localTopNHeap.size() < CompareParams.getTopN()) {
                this.localTopNHeap.add(otherResult);
            } else if (this.localTopNHeap.peek() != null && otherResult.getScore() > this.localTopNHeap.peek().getScore()) {
                this.localTopNHeap.poll();
                this.localTopNHeap.add(otherResult);
            }
        }
    }

    @Override
    public List<CompareResult> aggregate() {
        if (localTopNHeap == null) return Collections.emptyList();

        Map<String, List<CompareResult>> groupedMap = new HashMap<>();
        for (CompareResult result : localTopNHeap) {
            groupedMap.computeIfAbsent(result.getPsnTmplNo(), k -> new ArrayList<>()).add(result);
        }

        List<CompareResult> results = new ArrayList<>();
        for (List<CompareResult> list : groupedMap.values()) {
            if (list.isEmpty()) continue;

            list.sort((o1, o2) -> Float.compare(o2.getScore(), o1.getScore()));

            CompareResult maxResult = list.get(0);
            CompareResult minResult = list.get(list.size() - 1);

            CompareResult mergedResult = new CompareResult();
            mergedResult.setPsnTmplNo(maxResult.getPsnTmplNo());
            mergedResult.setFaceId(maxResult.getFaceId());
            mergedResult.setScore(maxResult.getScore());
            mergedResult.setMatched(maxResult.isMatched());

            mergedResult.setMaxFaceId(maxResult.getFaceId());
            mergedResult.setMaxScore(maxResult.getScore());

            mergedResult.setMinFaceId(minResult.getFaceId());
            mergedResult.setMinScore(minResult.getScore());

            List<CompareResult.compareDetails> allDetails = new ArrayList<>();
            for (CompareResult res : list) {
                if (res.getDetails() != null) {
                    allDetails.addAll(res.getDetails());
                }
            }
            mergedResult.setDetails(allDetails);

            results.add(mergedResult);
        }

        // 最终输出时，按分数从高到低排序
        results.sort((r1, r2) -> Float.compare(r2.getScore(), r1.getScore()));
        return results;
    }

}