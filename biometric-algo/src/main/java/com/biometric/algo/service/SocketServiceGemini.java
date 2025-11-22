package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.*;
import com.biometric.algo.socket.AlgoSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 算法服务接口实现类（Gemini版本）
 * 提供人脸识别算法的各类接口调用，包括:
 * 
 * - 人脸特征比对（特征vs特征、特征vs图片、图片vs图片）
 * - 人脸特征提取（标准提取、移动端提取、多人脸提取）
 * - 图像处理（人脸裁剪、去网格、人脸检测、质量评估）
 * 
 * 采用AlgoRequest和AlgoCommand封装请求参数，统一处理算法调用流程
 * 
 * @author biometric-algo
 * @version 1.0
 * @see AlgoSocketClient
 * @see AlgoRequest
 * @see AlgoCommand
 */
@Service
@RequiredArgsConstructor
public class SocketServiceGemini {

    /** Socket客户端，负责与算法引擎通信 */
    private final AlgoSocketClient socketClient;
    
    /** 算法配置，包含默认版本号等配置信息 */
    private final AlgoSocketConfig config;

    /**
     * 构建分组参数
     * 将图片或特征数据封装为标准的Group结构
     * 
     * @param dataMap 数据Map（图片或特征）
     * @param keyName 键名（"images"或"feature"）
     * @return 包含数据、算法类型和数量的Group对象
     */
    private JSONObject buildGroup(JSONObject dataMap, String keyName) {
        JSONObject group = new JSONObject();
        group.put(keyName, dataMap);
        group.put("algtype", 1); // 默认为1（可见光人脸）
        group.put("num", dataMap != null ? dataMap.size() : 0);
        return group;
    }

    /**
     * Y00.00 人脸特征比对 (特征 vs 特征)
     * 
     * @param featureMap1 第一组特征Map {"id": "base64..."}
     * @param featureMap2 第二组特征Map {"id": "base64..."}
     * @return 比对结果，包含每对特征的相似度
     */
    public SocketRecogResult faceCompareFeatToFeat(JSONObject featureMap1, JSONObject featureMap2) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.COMPARE_FEAT_TO_FEAT)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("PFEATURE1", buildGroup(featureMap1, "feature"));
        request.addParam("PFEATURE2", buildGroup(featureMap2, "feature"));

        return socketClient.execute(request, SocketRecogResult.class);
    }

    /**
     * Y00.01 人脸特征比对 (特征 vs 图片组)
     * @param featureMap1 特征Map {"id": "base64..."}
     * @param imageMap2 图片Map {"id": "base64..."}
     */
    public SocketRecogResult faceCompareFeatToImg(JSONObject featureMap1, JSONObject imageMap2) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.COMPARE_FEAT_TO_IMG)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("PFEATURE1", buildGroup(featureMap1, "feature"));
        request.addParam("PIMAGE2", buildGroup(imageMap2, "images"));

        return socketClient.execute(request, SocketRecogResult.class);
    }

    /**
     * Y00.02 人脸特征比对 (图片组 vs 图片组)
     * @param imageMap1 图片Map {"id": "base64..."}
     * @param imageMap2 图片Map {"id": "base64..."}
     */
    public SocketRecogResult faceCompareImgToImg(JSONObject imageMap1, JSONObject imageMap2) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.COMPARE_IMG_TO_IMG)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("PIMAGE1", buildGroup(imageMap1, "images"));
        request.addParam("PIMAGE2", buildGroup(imageMap2, "images"));

        return socketClient.execute(request, SocketRecogResult.class);
    }

    // ==================== Feature Extraction ====================

    /**
     * Y01.00 人脸特征提取 (JPG照片) - 默认参数
     */
    public SocketFaceFeature faceExtractFeature(JSONObject images) {
        return faceExtractFeature(images, true, false);
    }

    /**
     * Y01.00 人脸特征提取 (JPG照片) - 完整参数
     * @param images 图片Map {"id": "base64..."}
     * @param rotate 是否自动旋转
     * @param needQuality 是否需要质量评估
     */
    public SocketFaceFeature faceExtractFeature(JSONObject images, boolean rotate, boolean needQuality) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.EXTRACT_FEATURE)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size())
                .addParam("ROTATE", rotate)
                .addParam("QUALITY", needQuality);

        // 可以在 Client 层统一处理异常并返回特定的 Error Code，或者在这里处理
        return socketClient.execute(request, SocketFaceFeature.class);
    }

    /**
     * Y01.01 人脸特征提取 (移动终端，带人脸框) - 默认参数
     */
    public SocketFaceFeature faceExtractMobile(JSONObject images) {
        return faceExtractMobile(images, null, true);
    }

    /**
     * Y01.01 人脸特征提取 (移动终端，带人脸框) - 完整参数
     * @param images 图片Map {"id": "base64..."}
     * @param facesRect 人脸框位置（可选）
     * @param needQuality 是否需要质量评估
     */
    public SocketFaceFeature faceExtractMobile(JSONObject images, JSONObject facesRect, boolean needQuality) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.EXTRACT_MOBILE)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size())
                .addParam("QUALITY", needQuality);

        if (facesRect != null) {
            request.addParam("FACES", facesRect);
        }

        return socketClient.execute(request, SocketFaceFeature.class);
    }

    /**
     * Y01.02 人脸特征提取 (多人脸照片)
     * 注意：此接口输入是单张大图的Base64，返回包含多个人脸的特征数组
     * @param imageBase64 单张大图的Base64编码
     * @param needQuality 是否需要质量评估
     * @return 多人脸特征提取结果，RETURNVALUE为数组，每项包含face、feat、quality
     */
    public SocketMultiFaceFeature faceExtractMultiFace(String imageBase64, boolean needQuality) {
        // Y01.02接口需要使用NXFACE系列版本，不是FACE系列
        String version = config.getDefaultFaceVersion();
        if ("FACE310".equals(version)) {
            version = "NXFACEA102";
        }

        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.EXTRACT_MULTI)
                .version(version)
                .build();

        request.addParam("PIMAGE", imageBase64)
                .addParam("QUALITY", needQuality);

        return socketClient.execute(request, SocketMultiFaceFeature.class);
    }

    // ==================== Face Processing ====================

    /**
     * Y03.00 人脸裁剪 (标准裁剪)
     * @param imagesMap 图片数据组
     * @param width 裁剪后图片宽度
     * @param height 裁剪后图片高度
     * @param stdImg 是否输出标准图像
     */
    public SocketImageProcessResult faceCrop(JSONObject imagesMap, int width, int height, boolean stdImg) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.FACE_CROP)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", imagesMap)
                .addParam("NUM", imagesMap.size())
                .addParam("WIDTH", width)
                .addParam("HEIGHT", height)
                .addParam("STDIMG", stdImg);

        return socketClient.execute(request, SocketImageProcessResult.class);
    }

    /**
     * Y03.01 人脸裁剪 (带质量评估及阈值控制)
     * @param imagesMap 图片数据组
     * @param width 裁剪后图片宽度
     * @param height 裁剪后图片高度
     * @param thresholds 质量评估阈值配置（可选参数：MULTI、MAXDETECTSIZE、MINDETECTSIZE等）
     */
    public SocketImageProcessResult faceCropWithQuality(JSONObject imagesMap, int width, int height, JSONObject thresholds) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.FACE_CROP_WITH_QUALITY)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", imagesMap)
                .addParam("NUM", imagesMap.size())
                .addParam("WIDTH", width)
                .addParam("HEIGHT", height);

        if (thresholds != null) {
            for (String key : thresholds.keySet()) {
                request.addParam(key, thresholds.get(key));
            }
        }

        return socketClient.execute(request, SocketImageProcessResult.class);
    }

    /**
     * Y03.02 去网格
     * @param imagesMap 图片数据组
     */
    public SocketImageProcessResult imageRemoveGrid(JSONObject imagesMap) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.IMAGE_REMOVE_GRID)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", imagesMap)
                .addParam("NUM", imagesMap.size());

        return socketClient.execute(request, SocketImageProcessResult.class);
    }

    /**
     * Y03.03 人脸检测 (获取坐标和关键点)
     * @param imagesMap 图片数据组
     */
    public SocketFaceDetectionResult faceDetect(JSONObject imagesMap) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.FACE_DETECT)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", imagesMap)
                .addParam("NUM", imagesMap.size());

        return socketClient.execute(request, SocketFaceDetectionResult.class);
    }

    /**
     * Y03.04 人脸质量评估
     * @param imagesMap 图片数据组
     * @param facesRect 人脸框位置（可选）
     */
    public SocketFaceDetectResult faceQualityCheck(JSONObject imagesMap, JSONObject facesRect) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.QUALITY_CHECK)
                .version("QUALITY") // 特殊版本号
                .build();

        request.addParam("IMAGES", imagesMap)
                .addParam("NUM", imagesMap.size());

        if (facesRect != null) {
            request.addParam("FACES", facesRect);
        }

        return socketClient.execute(request, SocketFaceDetectResult.class);
    }

}