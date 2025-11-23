package com.biometric.algo.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PersonFaceData implements Serializable {
    private String personId;
    private String[] groupIds;
    private List<CachedFaceFeature> features;
}
