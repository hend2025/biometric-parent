package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.builder.AlgoRequestBuilder;
import com.biometric.algo.socket.SocketClient;

/**
 * Feature to Image comparison strategy (Y00.01)
 */
public class FeatureToImageStrategy extends FaceCompareStrategy {
    
    public FeatureToImageStrategy(SocketClient socketClient) {
        super(socketClient);
    }
    
    @Override
    protected JSONObject buildParams(JSONObject feature, JSONObject image, String version) {
        return AlgoRequestBuilder.newBuilder()
                .funId(getFunctionId())
                .algType(ALG_TYPE_FACE_VISIBLE)
                .version(version)
                .pFeature1(AlgoRequestBuilder.buildFeatureGroup(feature))
                .pImage2(AlgoRequestBuilder.buildImageGroup(image))
                .build();
    }
    
    @Override
    public String getFunctionId() {
        return "Y00.01";
    }
}
