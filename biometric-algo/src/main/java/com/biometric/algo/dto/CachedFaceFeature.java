package com.biometric.algo.dto;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

/**
 * 优化后的人脸特征缓存对象
 *
 * 【内存优化说明】
 * 原版本同时存储 featureData(512B) + binaryFeature(16B) + featureVector(512B) ≈ 1KB/特征
 * 优化后仅存储必要数据:
 * - binaryFeature: 用于汉明距离粗筛，16B
 * - featureVector: 用于余弦相似度精筛，512B
 * - featureData: 仅在需要NX算法时保留（可选）
 *
 * 预期内存节省: 约30-50%（取决于NX算法使用频率）
 */
public class CachedFaceFeature implements IdentifiedDataSerializable {

    private String faceId;

    /**
     * 原始特征数据 - 仅NX算法需要，其他场景设为null节省内存
     * @deprecated 建议使用 binaryFeature + featureVector 组合替代
     */
    private byte[] featureData;

    private String templateType;
    private String algoType;

    /**
     * 二进制量化特征 - 用于汉明距离快速粗筛
     * 固定长度: 4个int = 128bit = 16 bytes
     */
    private int[] binaryFeature;

    /**
     * 浮点特征向量 - 用于余弦相似度精确比对
     * 固定长度: 128个float = 512 bytes
     */
    private float[] featureVector;

    // ================== 内存优化标记 ==================
    /**
     * 是否保留原始特征数据的标记
     * 用于在序列化时判断是否需要写入featureData
     */
    private transient boolean keepRawData = false;

    public CachedFaceFeature() {
    }

    /**
     * 创建优化的特征对象（不保留原始数据）
     */
    public static CachedFaceFeature createOptimized(String faceId, int[] binaryFeat, float[] floatFeat) {
        CachedFaceFeature cf = new CachedFaceFeature();
        cf.faceId = faceId;
        cf.binaryFeature = binaryFeat;
        cf.featureVector = floatFeat;
        cf.keepRawData = false;
        return cf;
    }

    @Override
    public int getFactoryId() {
        return 1000;
    }

    @Override
    public int getClassId() {
        return 1;
    }

    /**
     * 优化的序列化方法
     * - 使用标记位指示是否存在featureData，避免空数组序列化开销
     * - 固定长度数组使用原始类型写入，减少元数据开销
     */
    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeString(faceId);
        out.writeString(templateType);
        out.writeString(algoType);

        // 使用标记位优化：1bit标记是否有featureData
        boolean hasRawData = (featureData != null && featureData.length > 0);
        out.writeBoolean(hasRawData);
        if (hasRawData) {
            out.writeByteArray(featureData);
        }

        // binaryFeature 固定4个int，直接写入避免长度字段
        if (binaryFeature != null && binaryFeature.length == 4) {
            out.writeBoolean(true);
            out.writeInt(binaryFeature[0]);
            out.writeInt(binaryFeature[1]);
            out.writeInt(binaryFeature[2]);
            out.writeInt(binaryFeature[3]);
        } else {
            out.writeBoolean(false);
        }

        // featureVector 使用标准数组写入（JIT会优化）
        out.writeFloatArray(featureVector);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        this.faceId = in.readString();
        this.templateType = in.readString();
        this.algoType = in.readString();

        // 读取可选的featureData
        boolean hasRawData = in.readBoolean();
        if (hasRawData) {
            this.featureData = in.readByteArray();
        }

        // 读取固定长度的binaryFeature
        boolean hasBinaryFeat = in.readBoolean();
        if (hasBinaryFeat) {
            this.binaryFeature = new int[4];
            this.binaryFeature[0] = in.readInt();
            this.binaryFeature[1] = in.readInt();
            this.binaryFeature[2] = in.readInt();
            this.binaryFeature[3] = in.readInt();
        }

        this.featureVector = in.readFloatArray();
    }

    /**
     * 计算此对象的估算内存占用（字节）
     * 用于监控和内存预算
     */
    public int estimateMemorySize() {
        int size = 40; // 对象头 + 引用字段开销
        if (faceId != null) size += 40 + faceId.length() * 2;
        if (templateType != null) size += 40 + templateType.length() * 2;
        if (algoType != null) size += 40 + algoType.length() * 2;
        if (featureData != null) size += 16 + featureData.length;
        if (binaryFeature != null) size += 16 + binaryFeature.length * 4;
        if (featureVector != null) size += 16 + featureVector.length * 4;
        return size;
    }

    // ================== Getters & Setters ==================

    public String getFaceId() { return faceId; }
    public void setFaceId(String faceId) { this.faceId = faceId; }

    public byte[] getFeatureData() { return featureData; }
    public void setFeatureData(byte[] featureData) { this.featureData = featureData; }

    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }

    public String getAlgoType() { return algoType; }
    public void setAlgoType(String algoType) { this.algoType = algoType; }

    public int[] getBinaryFeature() { return binaryFeature; }
    public void setBinaryFeature(int[] binaryFeature) { this.binaryFeature = binaryFeature; }

    public float[] getFeatureVector() { return featureVector; }
    public void setFeatureVector(float[] featureVector) { this.featureVector = featureVector; }

    public boolean isKeepRawData() { return keepRawData; }
    public void setKeepRawData(boolean keepRawData) { this.keepRawData = keepRawData; }

}