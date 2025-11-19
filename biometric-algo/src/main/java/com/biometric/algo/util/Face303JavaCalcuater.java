package com.biometric.algo.util;

public class Face303JavaCalcuater {
    private static final int DEFAULT_HAM_DIST = 50;
    private static final int FEATURE_LENGTH = 128;
    private static final int FEATURE_BYTE_SIZE = 512;
    
    private static int hamDist = DEFAULT_HAM_DIST;
    protected static int featureLength = FEATURE_LENGTH;

    public static float[] toFloatArray(byte[] byteArray) {
        if (byteArray == null) {
            throw new IllegalArgumentException("Byte array cannot be null");
        }
        if (byteArray.length != FEATURE_BYTE_SIZE) {
            throw new IllegalArgumentException("Byte array must be " + FEATURE_BYTE_SIZE + " bytes, got " + byteArray.length);
        }
        
        float[] result = new float[byteArray.length / 4];

        for(int i = 0; i < byteArray.length; i += 4) {
            int temp = byteArray[i];
            temp &= 255;
            temp = (int)((long)temp | (long)byteArray[i + 1] << 8);
            temp &= 65535;
            temp = (int)((long)temp | (long)byteArray[i + 2] << 16);
            temp &= 16777215;
            temp = (int)((long)temp | (long)byteArray[i + 3] << 24);
            result[i / 4] = Float.intBitsToFloat(temp);
        }

        return result;
    }

    public static float compare(float[] feat1, float[] feat2) {
        if (feat1 == null || feat2 == null) {
            throw new IllegalArgumentException("Feature arrays cannot be null");
        }
        if (feat1.length != FEATURE_LENGTH || feat2.length != FEATURE_LENGTH) {
            throw new IllegalArgumentException("Feature arrays must have length " + FEATURE_LENGTH);
        }
        
        float s1 = 0.0F;
        float s2 = 0.0F;
        float s3 = 0.0F;
        
        // Loop unrolling for better CPU pipeline utilization (process 4 elements at a time)
        int i = 0;
        for(; i < featureLength - 3; i += 4) {
            float tf1_0 = feat1[i];
            float tf2_0 = feat2[i];
            float tf1_1 = feat1[i + 1];
            float tf2_1 = feat2[i + 1];
            float tf1_2 = feat1[i + 2];
            float tf2_2 = feat2[i + 2];
            float tf1_3 = feat1[i + 3];
            float tf2_3 = feat2[i + 3];
            
            s1 += tf1_0 * tf1_0 + tf1_1 * tf1_1 + tf1_2 * tf1_2 + tf1_3 * tf1_3;
            s2 += tf2_0 * tf2_0 + tf2_1 * tf2_1 + tf2_2 * tf2_2 + tf2_3 * tf2_3;
            s3 += tf1_0 * tf2_0 + tf1_1 * tf2_1 + tf1_2 * tf2_2 + tf1_3 * tf2_3;
        }
        
        // Handle remaining elements
        for(; i < featureLength; ++i) {
            float tf1 = feat1[i];
            float tf2 = feat2[i];
            s1 += tf1 * tf1;
            s2 += tf2 * tf2;
            s3 += tf1 * tf2;
        }

        return s3 / (float)(Math.sqrt((double)(s1 * s2)) + 1.0E-6);
    }

    public static int[] getBinaFeat(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Input byte array cannot be null");
        }
        if (input.length != FEATURE_BYTE_SIZE) {
            throw new IllegalArgumentException("Input must be " + FEATURE_BYTE_SIZE + " bytes, got " + input.length);
        }
        int len = input.length / 128;
        int index = 0;
        int[] result = new int[len];
        int count = 0;
        int item = 0;

        for(int i = 3; i < input.length; i += 4) {
            if (input[i] < 0) {
                item &= Integer.MAX_VALUE;
            } else {
                item |= Integer.MIN_VALUE;
            }

            ++count;
            if (count >= 32) {
                count = 0;
                result[index++] = item;
                item = 0;
            } else {
                item >>= 1;
            }
        }

        return result;
    }

    public static boolean isBinaFeatSimilar(int feat11, int feat12, int feat13, int feat14, int feat21, int feat22, int feat23, int feat24) {
        int i = Integer.bitCount(feat11 ^ feat21) + Integer.bitCount(feat12 ^ feat22);
        if (i > hamDist) {
            return false;
        } else {
            i += Integer.bitCount(feat13 ^ feat23);
            if (i > hamDist) {
                return false;
            } else {
                i += Integer.bitCount(feat14 ^ feat24);
                return i <= hamDist;
            }
        }
    }

    public static boolean isBinaFeatSimilar(int feat11, int feat12, int feat13, int feat14, int feat21, int feat22, int feat23, int feat24, int customHamDist) {
        int i = Integer.bitCount(feat11 ^ feat21) + Integer.bitCount(feat12 ^ feat22);
        if (i > customHamDist) {
            return false;
        } else {
            i += Integer.bitCount(feat13 ^ feat23);
            if (i > customHamDist) {
                return false;
            } else {
                i += Integer.bitCount(feat14 ^ feat24);
                return i <= customHamDist;
            }
        }
    }

}