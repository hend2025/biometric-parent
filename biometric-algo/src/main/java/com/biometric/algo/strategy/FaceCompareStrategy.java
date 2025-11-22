package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.factory.ResponseFactory;
import com.biometric.algo.socket.ClaudeSocketClient;

/**
 * Feature to Feature comparison strategy
 */
public abstract class FaceCompareStrategy implements ComparisonStrategy {
    
    protected final ClaudeSocketClient socketClient;
    protected static final int ALG_TYPE_FACE_VISIBLE = 1;
    
    public FaceCompareStrategy(ClaudeSocketClient socketClient) {
        this.socketClient = socketClient;
    }
    
    @Override
    public SocketRecogResult compare(JSONObject data1, JSONObject data2, String version) {
        JSONObject params = buildParams(data1, data2, version);
        String jsonResponse = socketClient.sendRequest(params);
        // Use Factory Pattern for consistent response parsing
        return ResponseFactory.parseRecogResult(jsonResponse);
    }
    
    /**
     * Build request parameters (Template Method)
     */
    protected abstract JSONObject buildParams(JSONObject data1, JSONObject data2, String version);
}
