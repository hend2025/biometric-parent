package com.biometric.algo.dto;

import lombok.Data;

import java.util.Map;

/**
 * Recognition value data
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class RecogValue {
    
    private double avg;
    private double max;
    private double min;
    private Map<String, Integer> maxDetail;
    private Map<String, Integer> minDetail;
}
