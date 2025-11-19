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
    private static final int MAX_TOP_N = 100;
    private static final int MIN_TOP_N = 1;

    private final byte[] inputFeature;
    private final double threshold;
    private final int topN;
    private PriorityQueue<RecogResult> localTopNHeap;
    
    private transient float[] inputFloatFeature;
    private transient int[] inputBinaryFeature;

    public FaceRecogAggregator(byte[] inputFeature, double threshold, int topN) {
        if (inputFeature == null || inputFeature.length != 512) {
            throw new IllegalArgumentException("Input feature must be 512 bytes");
        }
        if (topN < MIN_TOP_N || topN > MAX_TOP_N) {
            throw new IllegalArgumentException("topN must be between " + MIN_TOP_N + " and " + MAX_TOP_N);
        }
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1");
        }

        this.inputFeature = inputFeature;
        this.threshold = threshold;
        this.topN = topN;
        initHeap();
    }

    private void initHeap() {
        this.localTopNHeap = new PriorityQueue<>(topN, new RecogResultScoreComparator());
        this.inputBinaryFeature = Face303JavaCalcuater.getBinaFeat(this.inputFeature);
        this.inputFloatFeature = Face303JavaCalcuater.toFloatArray(this.inputFeature);
    }

    @Override
    public void accumulate(Map.Entry<String, CachedFaceFeature> entry) {
        if (entry == null || entry.getValue() == null) {
            return;
        }

        CachedFaceFeature candidate = entry.getValue();
        
        if (inputBinaryFeature == null) {
            inputBinaryFeature = Face303JavaCalcuater.getBinaFeat(this.inputFeature);
            inputFloatFeature = Face303JavaCalcuater.toFloatArray(this.inputFeature);
        }

        int[] bFeat2 = candidate.getBinaryFeature();

        boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                inputBinaryFeature[0], inputBinaryFeature[1], inputBinaryFeature[2], inputBinaryFeature[3],
                bFeat2[0], bFeat2[1], bFeat2[2], bFeat2[3], 283
        );

        if (isSimilar) {
            float[] candidateFloatFeature = Face303JavaCalcuater.toFloatArray(candidate.getFeatureData());
            float similarity = Face303JavaCalcuater.compare(inputFloatFeature, candidateFloatFeature);

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
        if (aggregator == null) {
            return;
        }

        FaceRecogAggregator other = (FaceRecogAggregator) aggregator;
        if (other.localTopNHeap == null || other.localTopNHeap.isEmpty()) {
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
    
    private static class RecogResultScoreComparator implements Comparator<RecogResult>, Serializable {
        private static final long serialVersionUID = 1L;
        
        @Override
        public int compare(RecogResult r1, RecogResult r2) {
            return Double.compare(r1.getScore(), r2.getScore());
        }
    }

}