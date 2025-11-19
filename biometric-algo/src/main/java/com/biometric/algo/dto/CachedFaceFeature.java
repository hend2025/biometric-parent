package com.biometric.algo.dto;

import java.io.Serializable;
import java.util.Set;

public class CachedFaceFeature implements Serializable {
    private static final long serialVersionUID = 2L;
    private String faceId;
    private String psnTmplNo;
    private Set<String> groupIds;
    private byte[] featureData;
    private transient int[] binaryFeature;

    public CachedFaceFeature(String faceId, String psnTmplNo, byte[] featureData, Set<String> groupIds) {
        this.faceId = faceId;
        this.psnTmplNo = psnTmplNo;
        this.featureData = featureData;
        this.groupIds = groupIds;
    }

    public String getFaceId() { return faceId; }
    public String getPsnTmplNo() { return psnTmplNo; }
    public byte[] getFeatureData() { return featureData; }
    public Set<String> getGroupIds() { return groupIds; }
    

    public int[] getBinaryFeature() {
        if (binaryFeature == null && featureData != null) {
            binaryFeature = com.biometric.algo.util.Face303JavaCalcuater.getBinaFeat(featureData);
        }
        return binaryFeature;
    }

}