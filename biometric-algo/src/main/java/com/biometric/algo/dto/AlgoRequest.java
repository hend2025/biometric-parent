package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 算法请求封装类
 * 用于构建算法接口请求参数，支持Builder模式
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Data
@Builder
public class AlgoRequest {
    /** 算法命令 */
    private AlgoCommand command;
    
    /** 算法版本号 */
    private String version;
    
    /** 算法类型，默认1(可见光人脸) */
    @Builder.Default
    private int algType = 1;

    /** 业务参数容器 */
    @Builder.Default
    private JSONObject params = new JSONObject();

    /**
     * 添加请求参数
     * 
     * @param key 参数名
     * @param value 参数值
     * @return 当前请求对象，支持链式调用
     */
    public AlgoRequest addParam(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * 构建用于传输的JSON对象
     * 将请求参数和基础信息（FUNID、ALGTYPE、VERSION）合并为最终的传输JSON
     * 
     * @return 完整的请求JSON对象
     */
    public JSONObject toTransmissionJson() {
        JSONObject json = new JSONObject();
        json.putAll(this.params); // 放入业务参数
        json.put("FUNID", command.getFunId());
        json.put("ALGTYPE", algType);
        json.put("VERSION", version);
        return json;
    }

}