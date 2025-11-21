package com.biometric.algo.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Data
public class SocketResponse<T> {

    @JSONField(name = "RETURNID")
    private int returnId;

    @JSONField(name = "RETURNDESC")
    private String returnDesc;

    @JSONField(name = "RETURNVALUE", deserializeUsing = StringToObjDeserializer.class)
    private T returnValue;

    @Data
    public static class FaceFeatureValue {

        @JSONField(name = "MINSCORE")
        private double minScore;

        @JSONField(name = "DETAIL")
        private Map<String, Integer> detail;

        @JSONField(name = "ATTACHMENT")
        private Map<String, FaceFeatureValue.Attachment> attachment;

        @JSONField(name = "FEATURE")
        private FeatureData feature;

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
        public static class FeatureData {
            @JSONField(name = "algtype")
            private int algType;

            @JSONField(name = "feature", deserializeUsing = StringToObjDeserializer.class)
            private JSONObject featureValue;

            @JSONField(name = "iscompress")
            private int isCompress;

            @JSONField(name = "iscrypt")
            private int isCrypt;

            @JSONField(name = "num")
            private int num;

            @JSONField(name = "version")
            private String version;

            @JSONField(name = "type")
            private Map<String, FaceFeatureValue.TypeInfo> type;

        }

        @Data
        public static class TypeInfo {
            @JSONField(name = "direction")
            private int direction;

            @JSONField(name = "machine")
            private int machine;

        }

    }

    @Data
    public static class RecogValue {

        private double avg;

        private double max;

        private double min;

        private Map<String, Integer> maxDetail;

        private Map<String, Integer> minDetail;

    }

    @Data
    public static class FaceDetectValue {

        @JSONField(name = "DETAIL")
        private Map<String, Integer> detail;

        @JSONField(name = "VALUE")
        private Map<String, List<FaceInfo>> value;

        @Data
        public static class FaceInfo {
            @JSONField(name = "blurredquality")
            private int blurredQuality;

            @JSONField(name = "blurredscore")
            private double blurredScore;

            @JSONField(name = "face")
            private String face;

            @JSONField(name = "posequality")
            private int poseQuality;

            @JSONField(name = "posescore")
            private double poseScore;
        }
    }

    public static class StringToObjDeserializer implements ObjectDeserializer {
        @Override
        public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            String jsonString = parser.parseObject(String.class);
            return JSON.parseObject(jsonString, type);
        }

        @Override
        public int getFastMatchToken() {
            return 0;
        }
    }

}