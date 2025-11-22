package com.biometric.algo.builder;

import com.alibaba.fastjson.JSONObject;

/**
 * Builder Pattern: Constructs algorithm request parameters
 */
public class AlgoRequestBuilder {
    
    private final JSONObject params;
    private static final int ALG_TYPE_FACE_VISIBLE = 1;
    
    private AlgoRequestBuilder() {
        this.params = new JSONObject();
    }
    
    public static AlgoRequestBuilder newBuilder() {
        return new AlgoRequestBuilder();
    }
    
    public AlgoRequestBuilder funId(String funId) {
        params.put("FUNID", funId);
        return this;
    }
    
    public AlgoRequestBuilder algType(int algType) {
        params.put("ALGTYPE", algType);
        return this;
    }
    
    public AlgoRequestBuilder algType(String algType) {
        params.put("ALGTYPE", algType);
        return this;
    }
    
    public AlgoRequestBuilder version(String version) {
        params.put("VERSION", version);
        return this;
    }
    
    public AlgoRequestBuilder images(JSONObject images) {
        params.put("IMAGES", images);
        return this;
    }
    
    public AlgoRequestBuilder imageNum(int num) {
        params.put("NUM", num);
        return this;
    }
    
    public AlgoRequestBuilder rotate(boolean rotate) {
        params.put("ROTATE", rotate);
        return this;
    }
    
    public AlgoRequestBuilder quality(boolean quality) {
        params.put("QUALITY", quality);
        return this;
    }
    
    public AlgoRequestBuilder faces(JSONObject faces) {
        if (faces != null) {
            params.put("FACES", faces);
        }
        return this;
    }
    
    public AlgoRequestBuilder pImage(String imageBase64) {
        params.put("PIMAGE", imageBase64);
        return this;
    }
    
    public AlgoRequestBuilder pFeature1(JSONObject featureGroup) {
        params.put("PFEATURE1", featureGroup);
        return this;
    }
    
    public AlgoRequestBuilder pFeature2(JSONObject featureGroup) {
        params.put("PFEATURE2", featureGroup);
        return this;
    }
    
    public AlgoRequestBuilder pImage1(JSONObject imageGroup) {
        params.put("PIMAGE1", imageGroup);
        return this;
    }
    
    public AlgoRequestBuilder pImage2(JSONObject imageGroup) {
        params.put("PIMAGE2", imageGroup);
        return this;
    }
    
    public AlgoRequestBuilder width(int width) {
        params.put("WIDTH", width);
        return this;
    }
    
    public AlgoRequestBuilder height(int height) {
        params.put("HEIGHT", height);
        return this;
    }
    
    public AlgoRequestBuilder stdImg(boolean stdImg) {
        params.put("STDIMG", stdImg ? 1 : 0);
        return this;
    }
    
    public AlgoRequestBuilder thresholds(JSONObject thresholds) {
        if (thresholds != null) {
            params.putAll(thresholds);
        }
        return this;
    }
    
    public AlgoRequestBuilder customParam(String key, Object value) {
        params.put(key, value);
        return this;
    }
    
    public JSONObject build() {
        return params;
    }
    
    /**
     * Build data group (feature or image)
     */
    public static JSONObject buildGroup(JSONObject dataMap, int algType, String keyName) {
        JSONObject group = new JSONObject();
        group.put(keyName, dataMap);
        group.put("algtype", algType);
        group.put("num", dataMap != null ? dataMap.size() : 0);
        return group;
    }
    
    public static JSONObject buildFeatureGroup(JSONObject featureMap) {
        return buildGroup(featureMap, ALG_TYPE_FACE_VISIBLE, "feature");
    }
    
    public static JSONObject buildImageGroup(JSONObject imageMap) {
        return buildGroup(imageMap, ALG_TYPE_FACE_VISIBLE, "images");
    }
}
