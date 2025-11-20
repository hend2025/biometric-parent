package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Map;

@Data
public class FaceDataResponse {

    @JSONField(name = "MINSCORE")
    private double minScore;

    @JSONField(name = "DETAIL")
    private Map<String, Integer> detail;

    @JSONField(name = "ATTACHMENT")
    private Map<String, Attachment> attachment;

    @JSONField(name = "FEATURE")
    private FeatureInfo feature;

    @Data
    public static class Attachment {
        @JSONField(name = "iscolor")
        private boolean isColor;

        @JSONField(name = "picheight")
        private int picHeight;

        @JSONField(name = "picwidth")
        private int picWidth;

    }

    @Data
    public static class FeatureInfo {
        @JSONField(name = "algtype")
        private int algType;

        @JSONField(name = "feature")
        private String featureData;

        @JSONField(name = "iscompress")
        private int isCompress;

        @JSONField(name = "iscrypt")
        private int isCrypt;

        @JSONField(name = "num")
        private int num;

        @JSONField(name = "version")
        private String version;

        @JSONField(name = "type")
        private Map<String, TypeInfo> type;

    }

    @Data
    public static class TypeInfo {
        @JSONField(name = "direction")
        private int direction;

        @JSONField(name = "machine")
        private int machine;

    }

}
