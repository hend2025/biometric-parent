package com.biometric.algo.dto;

/**
 * 人脸特征提取结果响应类
 * 用于Y01.00/Y01.01等人脸特征提取接口的响应
 * 
 * 继承自SocketResponse，返回值类型为FaceFeatureValue，包含提取的人脸特征数据
 * 
 * @author biometric-algo
 * @version 1.0
 * @see FaceFeatureValue
 */
public class SocketFaceFeature extends SocketResponse<FaceFeatureValue> {

}
