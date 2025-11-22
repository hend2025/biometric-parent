package com.biometric.algo.dto;

/**
 * 人脸识别比对结果响应类
 * 用于Y00.00/Y00.01/Y00.02等人脸比对接口的响应
 * 
 * 继承自SocketResponse，返回值类型为RecogValue，包含比对相似度等信息
 * 
 * @author biometric-algo
 * @version 1.0
 * @see RecogValue
 */
public class SocketRecogResult extends SocketResponse<RecogValue> {

}