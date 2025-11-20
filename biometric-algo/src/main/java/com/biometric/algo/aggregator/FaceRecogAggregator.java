package com.biometric.algo.aggregator;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.ComparatorDetails; // 引入 ComparatorDetails
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

    // 定义汉明距离阈值
    private static final int HAM_DIST = 50;

    private transient List<float[]> inputFloatFeatures;
    private transient List<int[]> inputBinaryFeatures;

    private PriorityQueue<RecogResult> localTopNHeap;
    private RecogParam recogParam;

    public FaceRecogAggregator(RecogParam params) {
        this.recogParam = params;
        this.localTopNHeap = new PriorityQueue<>(params.getTopN(), new RecogResultScoreComparator());
        // 构造时尝试初始化，本地调用有效
        initInputFeatures();
    }

    private void initInputFeatures() {
        inputFloatFeatures = new ArrayList<>();
        inputBinaryFeatures = new ArrayList<>();

        if (recogParam != null && !CollectionUtils.isEmpty(recogParam.getFeatures())) {
            for (byte[] feature : recogParam.getFeatures()) {
                if (feature != null && feature.length > 0) {
                    int[] binaryFeat = Face303JavaCalcuater.getBinaFeat(feature);
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

        if (inputBinaryFeatures == null || inputFloatFeatures == null) {
            initInputFeatures();
        }

        if (inputBinaryFeatures.isEmpty()) {
            return;
        }

        CachedFaceFeature candidate = entry.getValue();
        int[] bFeat2 = candidate.getBinaryFeature();
        if (bFeat2 == null) {
            bFeat2 = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());
        }
        if (bFeat2 == null) {
            return;
        }

        float maxScore = -1.0f;
        float minScore = 2.0f; // 初始化为一个大于1的值，确保能被更新
        float[] candidateFloatFeature = null;
        boolean passedBinaryFilter = false;

        // 遍历所有输入特征
        for (int i = 0; i < inputBinaryFeatures.size(); i++) {
            int[] bFeat1 = inputBinaryFeatures.get(i);

            boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                    bFeat1[0], bFeat1[1], bFeat1[2], bFeat1[3],
                    bFeat2[0], bFeat2[1], bFeat2[2], bFeat2[3], HAM_DIST
            );

            if (isSimilar) {
                if (candidateFloatFeature == null) {
                    candidateFloatFeature = Face303JavaCalcuater.toFloatArray(candidate.getFeatureData());
                }

                float similarity = Face303JavaCalcuater.compare(inputFloatFeatures.get(i), candidateFloatFeature);

                // 更新最大分
                if (similarity > maxScore) {
                    maxScore = similarity;
                    passedBinaryFilter = true;
                }
                // 更新最小分 (只统计通过粗筛的)
                if (similarity < minScore) {
                    minScore = similarity;
                }
            }
        }

        // 如果通过了筛选且分数达标，加入结果堆
        if (passedBinaryFilter && maxScore >= recogParam.getThreshold()) {
            // 如果 minScore 没有被更新（说明只有一次比对或者初始值），则将其设为 maxScore，避免返回 2.0
            if (minScore > 1.0f) {
                minScore = maxScore;
            }
            addToHeap(candidate, maxScore, minScore);
        }
    }

    private void addToHeap(CachedFaceFeature candidate, float maxScore, float minScore) {
        if (localTopNHeap.size() < recogParam.getTopN()) {
            RecogResult result = buildResult(candidate, maxScore, minScore);
            localTopNHeap.add(result);
        } else if (maxScore > localTopNHeap.peek().getScore()) {
            localTopNHeap.poll();
            RecogResult result = buildResult(candidate, maxScore, minScore);
            localTopNHeap.add(result);
        }
    }

    private RecogResult buildResult(CachedFaceFeature candidate, float maxScore, float minScore) {
        RecogResult result = new RecogResult();
        result.setPsnTmplNo(candidate.getPsnTmplNo());
        result.setFaceId(candidate.getFaceId());
        result.setMatched(true);

        // 核心：填充比对分数字段
        result.setScore(maxScore);       // 主分数通常取最大值
        result.setMaxScore(maxScore);
        result.setMinScore(minScore);

        // 核心：填充FaceId字段
        // 由于是 1:N (多输入 vs 单候选人)，这里的 Max/Min FaceId 指向命中的候选人FaceId
        result.setMaxFaceId(candidate.getFaceId());
        result.setMinFaceId(candidate.getFaceId());

        // 核心：填充 Details
        ComparatorDetails details = new ComparatorDetails();
        details.setScore(maxScore);
        details.setFaceId2(candidate.getFaceId());
        // faceId1 代表输入源的ID，由于RecogParam仅传入特征列表无ID，此处可留空或根据业务需求传递索引
        // details.setFaceId1("input_index_x");
        result.setDetails(details);

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
        results.sort((r1, r2) -> Float.compare(r2.getScore(), r1.getScore()));
        return results;
    }

    private static class RecogResultScoreComparator implements Comparator<RecogResult>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public int compare(RecogResult r1, RecogResult r2) {
            return Float.compare(r1.getScore(), r2.getScore());
        }
    }

}