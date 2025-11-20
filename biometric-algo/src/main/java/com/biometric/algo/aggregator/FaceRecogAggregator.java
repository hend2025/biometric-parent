package com.biometric.algo.aggregator;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.RecogParam;
import com.biometric.algo.dto.RecogResult;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.hazelcast.aggregation.Aggregator;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;

public class FaceRecogAggregator
        implements Aggregator<Map.Entry<String, CachedFaceFeature>, List<RecogResult>>, Serializable {

    private static final long serialVersionUID = 1L;

    // 定义汉明距离阈值，用于快速过滤 (参考 Face303JavaCalcuater 中的默认值)
    private static final int HAM_DIST = 50;

    // 使用 transient 避免序列化，需在节点端延迟初始化
    private transient List<float[]> inputFloatFeatures;
    private transient List<int[]> inputBinaryFeatures;

    private PriorityQueue<RecogResult> localTopNHeap;
    private RecogParam recogParam;

    public FaceRecogAggregator(RecogParam params) {
        this.recogParam = params;
        // 初始化优先队列，容量为 topN
        this.localTopNHeap = new PriorityQueue<>(params.getTopN(), new RecogResultScoreComparator());
    }

    /**
     * 初始化输入特征列表。
     * 将 byte[] 类型的输入特征批量转换为 float[] 和 int[] 格式，避免在遍历中重复计算。
     */
    private void initInputFeatures() {
        inputFloatFeatures = new ArrayList<>();
        inputBinaryFeatures = new ArrayList<>();

        if (recogParam != null && !CollectionUtils.isEmpty(recogParam.getFeatures())) {
            for (byte[] feature : recogParam.getFeatures()) {
                if (feature != null && feature.length > 0) {
                    // 提取二进制特征用于快速过滤
                    int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(feature);
                    // 提取浮点特征用于精确比对
                    float[] floatFeat = Face303JavaCalcuater.toFloatArray(feature);

                    if (binaryFeat != null && floatFeat != null) {
                        inputBinaryFeatures.add(binaryFeat);
                        inputFloatFeatures.add(floatFeat);
                    }
                }
            }
        }
    }

    @Override
    public void accumulate(Map.Entry<String, CachedFaceFeature> entry) {
        if (entry == null || entry.getValue() == null) {
            return;
        }

        // 第一次执行时（或反序列化后）初始化输入特征列表
        if (inputBinaryFeatures == null || inputFloatFeatures == null) {
            initInputFeatures();
        }

        // 如果没有有效的输入特征，直接返回
        if (inputBinaryFeatures.isEmpty()) {
            return;
        }

        CachedFaceFeature candidate = entry.getValue();

        // 获取候选人的二进制特征 (优先从缓存对象中获取，没有则计算)
        int[] bFeat2 = candidate.getBinaryFeature();
        if (bFeat2 == null) {
            bFeat2 = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());
        }
        if (bFeat2 == null) {
            return;
        }

        float maxScore = -1.0f;
        float[] candidateFloatFeature = null;
        boolean passedBinaryFilter = false;

        // 遍历所有输入图片的特征，寻找与当前候选人匹配度最高的得分
        for (int i = 0; i < inputBinaryFeatures.size(); i++) {
            int[] bFeat1 = inputBinaryFeatures.get(i);

            // 1. 快速过滤 (汉明距离检查)
            // 使用 50 作为阈值，过滤掉差异过大的目标，提高效率
            boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                    bFeat1[0], bFeat1[1], bFeat1[2], bFeat1[3],
                    bFeat2[0], bFeat2[1], bFeat2[2], bFeat2[3], HAM_DIST
            );

            if (isSimilar) {
                // 2. 精确比对 (余弦相似度)
                // 只有在通过至少一次二进制过滤后，才将候选人的 byte[] 转为 float[]，且只转一次以节省开销
                if (candidateFloatFeature == null) {
                    candidateFloatFeature = Face303JavaCalcuater.toFloatArray(candidate.getFeatureData());
                }

                float similarity = Face303JavaCalcuater.compare(inputFloatFeatures.get(i), candidateFloatFeature);

                // 记录多张输入图片中匹配该候选人的最高分
                if (similarity > maxScore) {
                    maxScore = similarity;
                    passedBinaryFilter = true;
                }
            }
        }

        // 如果最高分超过阈值，且通过了二进制过滤，则加入结果堆
        if (passedBinaryFilter && maxScore >= recogParam.getThreshold()) {
            addToHeap(candidate, maxScore);
        }
    }

    /**
     * 将匹配结果加入并维护 TopN 堆
     */
    private void addToHeap(CachedFaceFeature candidate, float score) {
        if (localTopNHeap.size() < recogParam.getTopN()) {
            RecogResult result = buildResult(candidate, score);
            localTopNHeap.add(result);
        } else if (score > localTopNHeap.peek().getScore()) {
            localTopNHeap.poll();
            RecogResult result = buildResult(candidate, score);
            localTopNHeap.add(result);
        }
    }

    private RecogResult buildResult(CachedFaceFeature candidate, float score) {
        RecogResult result = new RecogResult();
        result.setPsnTmplNo(candidate.getPsnTmplNo());
        result.setFaceId(candidate.getFaceId());
        result.setScore(score);
        result.setMatched(true);
        return result;
    }

    @Override
    public void combine(Aggregator aggregator) {
        if (aggregator == null) {
            return;
        }

        FaceRecogAggregator other = (FaceRecogAggregator) aggregator;
        if (other.localTopNHeap == null || other.localTopNHeap.isEmpty()) {
            return;
        }

        for (RecogResult otherResult : other.localTopNHeap) {
            if (this.localTopNHeap.size() < recogParam.getTopN()) {
                this.localTopNHeap.add(otherResult);
            } else if (otherResult.getScore() > this.localTopNHeap.peek().getScore()) {
                this.localTopNHeap.poll();
                this.localTopNHeap.add(otherResult);
            }
        }
    }

    @Override
    public List<RecogResult> aggregate() {
        if (localTopNHeap == null || localTopNHeap.isEmpty()) {
            return new ArrayList<>();
        }
        List<RecogResult> results = new ArrayList<>(localTopNHeap);
        // 按分数降序排列
        results.sort((r1, r2) -> Float.compare(r2.getScore(), r1.getScore()));
        return results;
    }

    private static class RecogResultScoreComparator implements Comparator<RecogResult>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(RecogResult r1, RecogResult r2) {
            // 优先队列默认为小顶堆，保存TopN大的元素，堆顶应为最小值（即门槛值）
            return Float.compare(r1.getScore(), r2.getScore());
        }
    }

}