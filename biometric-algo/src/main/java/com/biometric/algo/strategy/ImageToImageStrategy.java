package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.builder.AlgoRequestBuilder;
import com.biometric.algo.socket.SocketClient;

/**
 * Image to Image comparison strategy (Y00.02)
 */
public class ImageToImageStrategy extends FaceCompareStrategy {
    
    public ImageToImageStrategy(SocketClient socketClient) {
        super(socketClient);
    }
    
    @Override
    protected JSONObject buildParams(JSONObject image1, JSONObject image2, String version) {
        return AlgoRequestBuilder.newBuilder()
                .funId(getFunctionId())
                .algType(ALG_TYPE_FACE_VISIBLE)
                .version(version)
                .pImage1(AlgoRequestBuilder.buildImageGroup(image1))
                .pImage2(AlgoRequestBuilder.buildImageGroup(image2))
                .build();
    }
    
    @Override
    public String getFunctionId() {
        return "Y00.02";
    }
}
