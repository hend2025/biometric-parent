package com.biometric.algo.util;

public class BiometricUtils {
    
    private static final int FEATURE_LENGTH = 512;
    private static final double NORMALIZATION_FACTOR = 512.0 * 128.0;
    
    public static double compare(byte[] feature1, byte[] feature2) {
        if (feature1 == null || feature2 == null) {
            return 0.0;
        }
        if (feature1.length != FEATURE_LENGTH || feature2.length != FEATURE_LENGTH) {
            return 0.0;
        }
        
        int diff = 0;
        int i = 0;
        
        for (; i <= FEATURE_LENGTH - 4; i += 4) {
            diff += Math.abs(feature1[i] - feature2[i]);
            diff += Math.abs(feature1[i + 1] - feature2[i + 1]);
            diff += Math.abs(feature1[i + 2] - feature2[i + 2]);
            diff += Math.abs(feature1[i + 3] - feature2[i + 3]);
        }
        
        for (; i < FEATURE_LENGTH; i++) {
            diff += Math.abs(feature1[i] - feature2[i]);
        }
        
        double similarity = 1.0 - (diff / NORMALIZATION_FACTOR);
        return Math.max(0.0, similarity);
    }

}