package com.biometric.algo.dto;

import java.io.Serializable;
import java.util.Set;

public class CachedFaceFeature implements Serializable {
    private static final long serialVersionUID = 3L;
    private String faceId;
    private String psnTmplNo;
    private Set<String> groupIds;
    private byte[] featureData;
    private int[] binaryFeature;

    public CachedFaceFeature(String faceId, String psnTmplNo, byte[] featureData, Set<String> groupIds) {
        this.faceId = faceId;
        this.psnTmplNo = psnTmplNo;
        this.featureData = featureData;
        this.groupIds = groupIds;
        if (featureData != null && featureData.length == 512) {
            this.binaryFeature = com.biometric.algo.util.Face303JavaCalcuater.getBinaFeat(featureData);
        }
    }

    public String getFaceId() { return faceId; }
    public String getPsnTmplNo() { return psnTmplNo; }
    public byte[] getFeatureData() { return featureData; }
    public Set<String> getGroupIds() { return groupIds; }
    

    public int[] getBinaryFeature() {
        return binaryFeature;
    }
    
    public void setBinaryFeature(int[] binaryFeature) {
        this.binaryFeature = binaryFeature;
    }
    
    public void setFaceId(String faceId) {
        this.faceId = faceId;
    }
    
    public void setPsnTmplNo(String psnTmplNo) {
        this.psnTmplNo = psnTmplNo;
    }
    
    public void setFeatureData(byte[] featureData) {
        this.featureData = featureData;
    }
    
    public void setGroupIds(Set<String> groupIds) {
        this.groupIds = groupIds;
    }

}