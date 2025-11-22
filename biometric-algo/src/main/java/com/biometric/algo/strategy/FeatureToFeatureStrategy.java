package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.builder.AlgoRequestBuilder;
import com.biometric.algo.socket.SocketClient;

/**
 * Feature to Feature comparison strategy (Y00.00)
 */
public class FeatureToFeatureStrategy extends FaceCompareStrategy {
    
    public FeatureToFeatureStrategy(SocketClient socketClient) {
        super(socketClient);
    }
    
    @Override
    protected JSONObject buildParams(JSONObject feature1, JSONObject feature2, String version) {
        return AlgoRequestBuilder.newBuilder()
                .funId(getFunctionId())
                .algType(ALG_TYPE_FACE_VISIBLE)
                .version(version)
                .pFeature1(AlgoRequestBuilder.buildFeatureGroup(feature1))
                .pFeature2(AlgoRequestBuilder.buildFeatureGroup(feature2))
                .build();
    }
    
    @Override
    public String getFunctionId() {
        return "Y00.00";
    }
}
