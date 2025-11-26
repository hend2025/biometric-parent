package com.biometric.algo.dto;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

public class CachedFaceFeature implements IdentifiedDataSerializable {

    private String faceId;
    private byte[] featureData;
    private String templateType;
    private String algoType;

    private int[] binaryFeature;
    private float[] featureVector;

    public CachedFaceFeature() {
    }

    @Override
    public int getFactoryId() {
        return 1000; // 自定义工厂ID
    }

    @Override
    public int getClassId() {
        return 1; // 类ID
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeString(faceId);
        out.writeByteArray(featureData);
        out.writeString(templateType);
        out.writeString(algoType);
        out.writeIntArray(binaryFeature);
        out.writeFloatArray(featureVector);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        this.faceId = in.readString();
        this.featureData = in.readByteArray();
        this.templateType = in.readString();
        this.algoType = in.readString();
        this.binaryFeature = in.readIntArray();
        this.featureVector = in.readFloatArray();
    }

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

}