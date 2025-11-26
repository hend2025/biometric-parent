package com.biometric.algo.util;

/**
 * 人脸特征向量计算工具类 (高性能优化版)
 * 针对 5000万+ 规模的 1:N 搜索进行了指令级优化
 */
public class Face303JavaCalcuater {

    // 汉明距离阈值 (默认50，越小越严格)
    private static int hamDist = 50;
    // 特征向量长度 (固定为128维)
    private static int featureLength = 128;

    /**
     * 将 byte[] 原始特征转换为 float[] 向量
     * 使用位运算加速转换过程
     */
    public static float[] toFloatArray(byte[] byteArray) {
        if (byteArray == null) return null;

        float[] result = new float[byteArray.length / 4];

        // 每次处理 4 个字节转换为 1 个 float
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

    /**
     * 计算两个浮点向量的余弦相似度
     * [极致优化]: 采用循环展开 (Loop Unrolling) 技术，步长为 8
     */
    public static float compare(float[] feat1, float[] feat2) {
        if (feat1 == null || feat2 == null) {
            return 0.0F;
        }

        float s1 = 0.0F;
        float s2 = 0.0F;
        float s3 = 0.0F;

        int len = feat1.length;
        int i = 0;

        // --- 核心优化区：循环展开 ---
        // 每次迭代处理 8 个维度，利用 CPU 的超标量架构并行执行乘加指令
        // 128维特征正好循环 16 次，无余数，但为了稳健性保留尾部处理
        for (; i <= len - 8; i += 8) {
            float v1_0 = feat1[i];   float v2_0 = feat2[i];
            float v1_1 = feat1[i+1]; float v2_1 = feat2[i+1];
            float v1_2 = feat1[i+2]; float v2_2 = feat2[i+2];
            float v1_3 = feat1[i+3]; float v2_3 = feat2[i+3];
            float v1_4 = feat1[i+4]; float v2_4 = feat2[i+4];
            float v1_5 = feat1[i+5]; float v2_5 = feat2[i+5];
            float v1_6 = feat1[i+6]; float v2_6 = feat2[i+6];
            float v1_7 = feat1[i+7]; float v2_7 = feat2[i+7];

            // 累加平方和 (Self Dot Product)
            s1 += v1_0 * v1_0 + v1_1 * v1_1 + v1_2 * v1_2 + v1_3 * v1_3 +
                    v1_4 * v1_4 + v1_5 * v1_5 + v1_6 * v1_6 + v1_7 * v1_7;

            s2 += v2_0 * v2_0 + v2_1 * v2_1 + v2_2 * v2_2 + v2_3 * v2_3 +
                    v2_4 * v2_4 + v2_5 * v2_5 + v2_6 * v2_6 + v2_7 * v2_7;

            // 累加点积 (Dot Product)
            s3 += v1_0 * v2_0 + v1_1 * v2_1 + v1_2 * v2_2 + v1_3 * v2_3 +
                    v1_4 * v2_4 + v1_5 * v2_5 + v1_6 * v2_6 + v1_7 * v2_7;
        }

        // 处理尾部剩余数据 (防止特征长度不是 8 的倍数)
        for (; i < len; ++i) {
            float tf1 = feat1[i];
            float tf2 = feat2[i];
            s1 += tf1 * tf1;
            s2 += tf2 * tf2;
            s3 += tf1 * tf2;
        }

        // 计算分母，增加极小值防止除以零
        double denominator = Math.sqrt((double)(s1 * s2));
        if (denominator < 1.0E-9) {
            return 0.0F;
        }

        return (float)(s3 / denominator);
    }

    /**
     * 提取二进制特征 (用于汉明距离粗筛)
     * 将 512字节 的 float 特征压缩量化为 int[] 数组
     */
    public static int[] getBinaFeat(byte[] input) {
        // 校验输入长度，标准特征长度通常为 512 字节
        if (null != input && 512 == input.length) {
            int len = input.length / 128; // 512 / 128 = 4 个 int
            int index = 0;
            int[] result = new int[len];
            int count = 0;
            int item = 0;

            // 每隔 4 个字节采样一次符号位进行量化
            for(int i = 3; i < input.length; i += 4) {
                if (input[i] < 0) {
                    item &= Integer.MAX_VALUE; // 设为0
                } else {
                    item |= Integer.MIN_VALUE; // 设为1
                }

                ++count;
                if (count >= 32) { // 凑够 32 位生成一个 int
                    count = 0;
                    result[index++] = item;
                    item = 0;
                } else {
                    item >>= 1; // 移位
                }
            }

            return result;
        } else {
            return null;
        }
    }

    /**
     * 计算汉明距离并判断是否相似 (使用默认阈值)
     * 利用 CPU 的 POPCNT 指令极速计算位差异
     */
    public static boolean isBinaFeatSimilar(int feat11, int feat12, int feat13, int feat14,
                                            int feat21, int feat22, int feat23, int feat24) {
        return isBinaFeatSimilar(feat11, feat12, feat13, feat14,
                feat21, feat22, feat23, feat24, hamDist);
    }

    /**
     * 计算汉明距离并判断是否相似 (指定阈值)
     */
    public static boolean isBinaFeatSimilar(int feat11, int feat12, int feat13, int feat14,
                                            int feat21, int feat22, int feat23, int feat24,
                                            int customHamDist) {
        // Integer.bitCount 对应 x86 的 POPCNT 指令，单周期执行，极快
        int dist = Integer.bitCount(feat11 ^ feat21) +
                Integer.bitCount(feat12 ^ feat22);

        // 快速剪枝：如果前两段差距已经过大，直接返回 false
        if (dist > customHamDist) return false;

        dist += Integer.bitCount(feat13 ^ feat23);
        if (dist > customHamDist) return false;

        dist += Integer.bitCount(feat14 ^ feat24);
        return dist <= customHamDist;
    }

}