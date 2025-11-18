package com.biometric.algo.dto;

import java.io.Serializable;
import java.util.Set;

public class CachedFaceFeature implements Serializable {
    private static final long serialVersionUID = 1L;
    private String faceId;
    private String psnTmplNo;
    private byte[] featureData;
    private Set<String> groupIds;

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

}