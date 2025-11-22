package com.biometric.algo.dto.deserializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.Type;

/**
 * Custom deserializer for string to object conversion
 */
public class StringToObjectDeserializer implements ObjectDeserializer {
    
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
