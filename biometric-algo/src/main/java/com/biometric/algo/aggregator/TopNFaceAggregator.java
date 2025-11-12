package com.biometric.algo.aggregator;

import com.biometric.algo.model.FaceFeature;
import com.biometric.algo.model.FaceMatchResult;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.hazelcast.aggregation.Aggregator;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class TopNFaceAggregator implements Aggregator<Entry<String, FaceFeature>, List<FaceMatchResult>>, Serializable {

    private byte[] targetFeature;
    private float threshold;
    private int topN;
    private PriorityQueue<FaceMatchResult> results;

    public TopNFaceAggregator(byte[] targetFeature, float threshold, int topN) {
        this.targetFeature = targetFeature;
        this.threshold = threshold;
        this.topN = topN;
        this.results = new PriorityQueue<>(new FaceMatchResultComparator());
    }

    @Override
    public void accumulate(Entry<String, FaceFeature> entry) {
        FaceFeature feature = entry.getValue();
        int[] bFeat1 = Face303JavaCalcuater.getBinaFeat(targetFeature);
        int[] bFeat2 = Face303JavaCalcuater.getBinaFeat(feature.getFeatureVector());
        
        boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                bFeat1[0], bFeat1[1], bFeat1[2], bFeat1[3],
                bFeat2[0], bFeat2[1], bFeat2[2], bFeat2[3], 283
        );
        
        if (isSimilar) {
            float similarity = Face303JavaCalcuater.compare(
                Face303JavaCalcuater.toFloatArray(targetFeature),
                Face303JavaCalcuater.toFloatArray(feature.getFeatureVector())
            );

            if (similarity >= threshold) {
                FaceMatchResult result = new FaceMatchResult();
                result.setFaceId(feature.getFaceId());
                result.setPsnNo(feature.getPsnNo());
                result.setSimilarity(similarity);
                result.setMatched(true);

                results.offer(result);
                
                if (results.size() > topN) {
                    results.poll();
                }
            }
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        TopNFaceAggregator other = (TopNFaceAggregator) aggregator;
        for (FaceMatchResult result : other.results) {
            results.offer(result);
            if (results.size() > topN) {
                results.poll();
            }
        }
    }

    @Override
    public List<FaceMatchResult> aggregate() {
        List<FaceMatchResult> finalResults = new ArrayList<>(results);
        finalResults.sort(new FaceMatchResultComparator().reversed());
        return finalResults;
    }
    
    private static class FaceMatchResultComparator implements Comparator<FaceMatchResult>, Serializable {
        @Override
        public int compare(FaceMatchResult a, FaceMatchResult b) {
            return Double.compare(a.getSimilarity(), b.getSimilarity());
        }
    }

}