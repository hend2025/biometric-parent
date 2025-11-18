package com.biometric.algo.dto;

import java.io.Serializable;
import java.util.Comparator;

public class RecogResult implements Serializable, Comparable<RecogResult> {
    private static final long serialVersionUID = 1L;
    private String matchedPsnTmplNo;
    private String matchedFaceId;
    private float score;
    private boolean matched;

    public RecogResult() {}

    public RecogResult(String matchedPsnTmplNo, String matchedFaceId, float score) {
        this.matchedPsnTmplNo = matchedPsnTmplNo;
        this.matchedFaceId = matchedFaceId;
        this.score = score;
    }
    public String getMatchedPsnTmplNo() { return matchedPsnTmplNo; }
    public String getMatchedFaceId() { return matchedFaceId; }
    public double getScore() { return score; }

    @Override
    public int compareTo(RecogResult other) {
        return Double.compare(this.score, other.score);
    }

    public static Comparator<RecogResult> maxScoreComparator() {
        return (r1, r2) -> Double.compare(r2.getScore(), r1.getScore());
    }

}