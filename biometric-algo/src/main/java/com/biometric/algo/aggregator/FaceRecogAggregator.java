package com.biometric.algo.aggregator;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.CompareParams;
import com.biometric.algo.dto.CompareResult;
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
public class FaceRecogAggregator implements Aggregator<Map.Entry<String, CachedFaceFeature>, List<CompareResult>>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HAMMING_DIST_THRESHOLD = 50;

    // 输入特征（不需要序列化传输到各节点，但在节点本地反序列化后使用）
    private transient List<float[]> inputFloatFeatures;
    private transient List<int[]> inputBinaryFeatures;

    // 结果堆（最小堆，保留TopN）
    private PriorityQueue<CompareResult> localTopNHeap;
    private CompareParams recogParam;

    // 静态内部类实现可序列化的比较器
    private static class CompareResultComparator implements Comparator<CompareResult>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(CompareResult r1, CompareResult r2) {
            return Double.compare(r1.getScore(), r2.getScore());
        }
    }

    public FaceRecogAggregator(CompareParams params) {
        this.recogParam = params;
        this.localTopNHeap = new PriorityQueue<>(params.getTopN(), new CompareResultComparator());
        // 注意：构造函数是在调用端执行的，initInputFeatures 可能需要在 accumulate 首次调用时再次检查
        initInputFeatures();
    }

    private void initInputFeatures() {
        if (inputFloatFeatures != null && inputBinaryFeatures != null) {
            return;
        }
        inputFloatFeatures = new ArrayList<>();
        inputBinaryFeatures = new ArrayList<>();

        if (recogParam != null && !CollectionUtils.isEmpty(recogParam.getFeatures())) {
            for (byte[] feature : recogParam.getFeatures()) {
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

    @Override
    public void accumulate(Map.Entry<String, CachedFaceFeature> entry) {
        if (entry == null || entry.getValue() == null) return;

        // 确保输入特征在当前节点已初始化
        if (inputBinaryFeatures == null) {
            initInputFeatures();
        }
        if (inputBinaryFeatures.isEmpty()) return;

        CachedFaceFeature candidate = entry.getValue();

        // 1. 获取候选人二进制特征（优先使用缓存的）
        int[] candidateBinaryFeat = candidate.getBinaryFeature();
        if (candidateBinaryFeat == null) {
            candidateBinaryFeat = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());
        }
        if (candidateBinaryFeat == null) return;

        float maxScore = -1.0f;
        float minScore = 2.0f; // Cosine相似度最大为1.0，初始设为2.0作为"无穷大"
        float[] candidateFloatFeat = null;
        boolean passedBinaryFilter = false;

        // 2. 遍历所有输入特征进行比对（1:N 中的 N 对比输入的多张人脸）
        for (int i = 0; i < inputBinaryFeatures.size(); i++) {
            int[] inputBFeat = inputBinaryFeatures.get(i);

            // 2.1 粗筛：汉明距离检查
            boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                    inputBFeat[0], inputBFeat[1], inputBFeat[2], inputBFeat[3],
                    candidateBinaryFeat[0], candidateBinaryFeat[1], candidateBinaryFeat[2], candidateBinaryFeat[3],
                    HAMMING_DIST_THRESHOLD
            );

            if (isSimilar) {
                // 2.2 精筛：余弦相似度计算
                // 懒加载：只有通过粗筛才转换浮点数组
                if (candidateFloatFeat == null) {
                    candidateFloatFeat = Face303JavaCalcuater.toFloatArray(candidate.getFeatureData());
                }

                float similarity = Face303JavaCalcuater.compare(inputFloatFeatures.get(i), candidateFloatFeat);

                // 记录最大相似度
                if (similarity > maxScore) {
                    maxScore = similarity;
                    passedBinaryFilter = true;
                }
                if (similarity < minScore) {
                    minScore = similarity;
                }
            }
        }

        // 3. 阈值判断与堆更新
        if (passedBinaryFilter && maxScore >= recogParam.getThreshold()) {
            if (minScore > 1.0f) minScore = maxScore; // 修正未初始化的 minScore
            updateHeap(candidate, maxScore, minScore);
        }
    }

    private void updateHeap(CachedFaceFeature candidate, float maxScore, float minScore) {
        // 如果堆未满，直接添加
        if (localTopNHeap.size() < recogParam.getTopN()) {
            localTopNHeap.add(buildResult(candidate, maxScore, minScore));
        }
        // 如果堆满了，且当前分数高于堆顶（堆顶是最小的），则替换
        else if (localTopNHeap.peek() != null && maxScore > localTopNHeap.peek().getScore()) {
            localTopNHeap.poll();
            localTopNHeap.add(buildResult(candidate, maxScore, minScore));
        }
    }

    private CompareResult buildResult(CachedFaceFeature candidate, float maxScore, float minScore) {
        CompareResult result = new CompareResult();
        result.setPsnTmplNo(candidate.getPsnTmplNo());
        result.setFaceId(candidate.getFaceId());
        result.setMatched(true);
        result.setScore(maxScore);
        result.setMaxScore(maxScore);
        result.setMinScore(minScore);
        result.setMaxFaceId(candidate.getFaceId());
        result.setMinFaceId(candidate.getFaceId());

        CompareResult.compareDetails details = new CompareResult.compareDetails();
        details.setScore(maxScore);
        details.setFaceId2(candidate.getFaceId());
        result.setDetails(details);

        return result;
    }

    @Override
    public void combine(Aggregator aggregator) {
        if (!(aggregator instanceof FaceRecogAggregator)) return;

        FaceRecogAggregator other = (FaceRecogAggregator) aggregator;
        if (other.localTopNHeap == null) return;

        // 合并其他分片的 TopN 结果
        for (CompareResult otherResult : other.localTopNHeap) {
            // 复用堆更新逻辑
            if (this.localTopNHeap.size() < recogParam.getTopN()) {
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

        // 最终输出时，按分数从高到低排序
        List<CompareResult> results = new ArrayList<>(localTopNHeap);
        results.sort((r1, r2) -> Float.compare(r2.getScore(), r1.getScore()));
        return results;
    }

}