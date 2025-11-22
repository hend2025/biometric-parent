package com.biometric.algo.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 通用嵌套JSON反序列化器
 * 用于处理字段值是 "JSON字符串" 的情况，将其自动解析为目标对象 T
 */
@Slf4j
public class NestedJsonDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private JavaType targetType;

    public NestedJsonDeserializer() {
    }

    public NestedJsonDeserializer(JavaType targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取目标字段的类型信息
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType type = property != null ? property.getType() : ctxt.getContextualType();
        return new NestedJsonDeserializer(type);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 1. 获取当前字段的值
        String jsonString = p.getText();

        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        // 2. 获取 ObjectMapper (复用 Context 中的 codec)
        ObjectMapper mapper = (ObjectMapper) p.getCodec();

        try {
            // 3. 将字符串再次解析为目标类型
            return mapper.readValue(jsonString, targetType);
        } catch (Exception e) {
            log.error("嵌套JSON解析失败: {}", jsonString, e);
            // 即使解析失败，也尽量不阻断流程，或者根据业务需求抛出异常
            return null;
        }
    }

}