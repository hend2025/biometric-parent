package com.biometric.algo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RecogStatsData {

    private double avg;

    private double max;

    private double min;

    private Map<String, Integer> maxDetail;

    private Map<String, Integer> minDetail;

}
