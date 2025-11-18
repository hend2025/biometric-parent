package com.biometric.algo.aggregator;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.RecogResult;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.hazelcast.aggregation.Aggregator;

import java.io.Serializable;
import java.util.*;

public class FaceRecogAggregator
        implements Aggregator<Map.Entry<String, CachedFaceFeature>, List<RecogResult>>, Serializable {

    private static final long serialVersionUID = 1L;
    private final byte[] inputFeature;
    private final Set<String> targetGroupIds;
    private final double threshold;
    private final int topN;
    private transient PriorityQueue<RecogResult> localTopNHeap;

    public FaceRecogAggregator(byte[] inputFeature, Set<String> targetGroupIds, double threshold, int topN) {
        this.inputFeature = inputFeature;
        this.targetGroupIds = targetGroupIds;
        this.threshold = threshold;
        this.topN = topN;
        initHeap();
    }

    private void initHeap() {
        this.localTopNHeap = new PriorityQueue<>(topN, Comparator.comparingDouble(RecogResult::getScore));
    }

    @Override
    public void accumulate(Map.Entry<String, CachedFaceFeature> entry) {
        if (localTopNHeap == null) {
            initHeap();
        }
        CachedFaceFeature candidate = entry.getValue();

        int[] bFeat1 = Face303JavaCalcuater.getBinaFeat(this.inputFeature);
        int[] bFeat2 = Face303JavaCalcuater.getBinaFeat(candidate.getFeatureData());

        boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                bFeat1[0], bFeat1[1], bFeat1[2], bFeat1[3],
                bFeat2[0], bFeat2[1], bFeat2[2], bFeat2[3], 283
        );

        if (isSimilar) {
            float similarity = Face303JavaCalcuater.compare(
                    Face303JavaCalcuater.toFloatArray(this.inputFeature),
                    Face303JavaCalcuater.toFloatArray(candidate.getFeatureData())
            );

            if (similarity >= threshold) {
                if (localTopNHeap.size() < topN) {
                    localTopNHeap.add(new RecogResult(
                            candidate.getPsnTmplNo(), candidate.getFaceId(), similarity
                    ));
                } else if (similarity > localTopNHeap.peek().getScore()) {
                    localTopNHeap.poll();
                    localTopNHeap.add(new RecogResult(
                            candidate.getPsnTmplNo(), candidate.getFaceId(), similarity
                    ));
                }
            }
        }

    }

    @Override
    public void combine(Aggregator aggregator) {
        if (localTopNHeap == null) {
            initHeap();
        }
        FaceRecogAggregator other = (FaceRecogAggregator) aggregator;
        if (other.localTopNHeap == null) {
            return;
        }
        for (RecogResult otherResult : other.localTopNHeap) {
            if (this.localTopNHeap.size() < topN) {
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
        results.sort(RecogResult.maxScoreComparator());
        return results;
    }

}