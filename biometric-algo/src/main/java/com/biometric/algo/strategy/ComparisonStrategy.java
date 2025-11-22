package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.SocketRecogResult;

/**
 * 比对策略接口
 * 采用策略模式定义不同类型的人脸比对策略（特征vs特征、特征vs图片、图片vs图片）
 * 
 * @author biometric-algo
 * @version 1.0
 */
public interface ComparisonStrategy {
    
    /**
     * 执行比对操作
     * 
     * @param data1 第一组数据对象（特征或图片）
     * @param data2 第二组数据对象（特征或图片）
     * @param version 算法版本号
     * @return 比对结果
     */
    SocketRecogResult compare(JSONObject data1, JSONObject data2, String version);
    
    /**
     * 获取策略对应的功能ID
     * 
     * @return 功能ID（如Y00.00、Y00.01、Y00.02）
     */
    String getFunctionId();
}
