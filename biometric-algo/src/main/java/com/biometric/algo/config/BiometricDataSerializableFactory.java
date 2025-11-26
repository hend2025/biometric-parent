package com.biometric.algo.config;

import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.PersonFaceData;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class BiometricDataSerializableFactory implements DataSerializableFactory {

    public static final int FACTORY_ID = 1000;

    public static final int ID_CACHED_FACE_FEATURE = 1;
    public static final int ID_PERSON_FACE_DATA = 2;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        switch (typeId) {
            case ID_CACHED_FACE_FEATURE:
                return new CachedFaceFeature();
            case ID_PERSON_FACE_DATA:
                return new PersonFaceData();
            default:
                return null;
        }
    }

}