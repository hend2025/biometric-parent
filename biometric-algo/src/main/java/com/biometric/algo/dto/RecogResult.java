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

    public RecogResult(String matchedPsnTmplNo, String matchedFaceId, float score, boolean matched) {
        this.matchedPsnTmplNo = matchedPsnTmplNo;
        this.matchedFaceId = matchedFaceId;
        this.score = score;
        this.matched = matched;
    }
    public String getMatchedPsnTmplNo() { return matchedPsnTmplNo; }
    public String getMatchedFaceId() { return matchedFaceId; }
    public double getScore() { return score; }
    public boolean isMatched() { return matched; }
    
    public void setMatchedPsnTmplNo(String matchedPsnTmplNo) { this.matchedPsnTmplNo = matchedPsnTmplNo; }
    public void setMatchedFaceId(String matchedFaceId) { this.matchedFaceId = matchedFaceId; }
    public void setScore(float score) { this.score = score; }
    public void setMatched(boolean matched) { this.matched = matched; }

    @Override
    public int compareTo(RecogResult other) {
        return Double.compare(this.score, other.score);
    }

    public static Comparator<RecogResult> maxScoreComparator() {
        return (r1, r2) -> Double.compare(r2.getScore(), r1.getScore());
    }
    
    @Override
    public String toString() {
        return "RecogResult{" +
                "psnTmplNo='" + matchedPsnTmplNo + '\'' +
                ", faceId='" + matchedFaceId + '\'' +
                ", score=" + score +
                ", matched=" + matched +
                '}';
    }

}