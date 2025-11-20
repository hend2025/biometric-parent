package com.biometric.algo.dto;

import java.util.List;

@lombok.Data
public class RecogParam {

    List<byte[]> features;

    List<String> images;

    List<String> groups;

    String algoType;

    float threshold;

    int topN;

}
