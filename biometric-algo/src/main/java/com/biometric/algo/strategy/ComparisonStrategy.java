package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.SocketRecogResult;

public abstract class ComparisonStrategy {
    /** 算法类型 - 可见光人脸 */
    private static final int ALG_TYPE_FACE_VISIBLE = 1;

    public abstract SocketRecogResult compare(JSONObject data1, JSONObject data2, String version);

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