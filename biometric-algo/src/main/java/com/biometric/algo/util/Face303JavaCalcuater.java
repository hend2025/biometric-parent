package com.biometric.algo.util;

public class Face303JavaCalcuater {

    private static int hamDist = 50;
    private static int featureLength = 128;

    public static float[] toFloatArray(byte[] byteArray) {
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
        float Cos_score = 0.0F;
        float s1 = 0.0F;
        float s2 = 0.0F;
        float s3 = 0.0F;
        if (null != feat1 && null != feat2) {
            for(int i = 0; i < featureLength; ++i) {
                float tf1 = feat1[i];
                float tf2 = feat2[i];
                s1 += tf1 * tf1;
                s2 += tf2 * tf2;
                s3 += tf1 * tf2;
            }

            Cos_score = s3 / (float)(Math.sqrt((double)(s1 * s2)) + 1.0E-6);
        }

        return Cos_score;
    }

    public static int[] getBinaFeat(byte[] input) {
        if (null != input && 512 == input.length) {
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
        } else {
            return null;
        }
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
