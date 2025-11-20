package com.biometric.algo.dto;

import java.io.Serializable;
import java.util.List;

@lombok.Data
public class RecogParam implements Serializable {

    List<byte[]> features;

    List<String> images;

    List<String> groups;

    String algoType;

    float threshold;

    int topN;

}
