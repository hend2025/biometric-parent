package com.biometric.algo.dto;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PersonFaceData implements IdentifiedDataSerializable {

    private String personId;
    private String[] groupIds;
    private List<CachedFaceFeature> features;

    public PersonFaceData() {
    }

    @Override
    public int getFactoryId() {
        return 1000;
    }

    @Override
    public int getClassId() {
        return 2;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeString(personId);
        out.writeStringArray(groupIds);

        if (features == null) {
            out.writeInt(0);
        } else {
            out.writeInt(features.size());
            for (CachedFaceFeature feature : features) {
                out.writeObject(feature);
            }
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        this.personId = in.readString();
        this.groupIds = in.readStringArray();

        int size = in.readInt();
        if (size > 0) {
            this.features = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                this.features.add(in.readObject());
            }
        } else {
            this.features = new ArrayList<>();
        }
    }

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String[] getGroupIds() { return groupIds; }
    public void setGroupIds(String[] groupIds) { this.groupIds = groupIds; }
    public List<CachedFaceFeature> getFeatures() { return features; }
    public void setFeatures(List<CachedFaceFeature> features) { this.features = features; }
}