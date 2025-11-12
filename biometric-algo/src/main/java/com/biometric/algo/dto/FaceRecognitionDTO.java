package com.biometric.algo.dto;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import lombok.Data;

import java.io.IOException;

/**
 * 人脸识别DTO
 */
@Data
public class FaceRecognitionDTO implements DataSerializable {

    private byte[] featureVector;

    private String queryImageUrl;

    @Override
    public void writeData(ObjectDataOutput objectDataOutput) throws IOException {

    }

    @Override
    public void readData(ObjectDataInput objectDataInput) throws IOException {

    }

}

