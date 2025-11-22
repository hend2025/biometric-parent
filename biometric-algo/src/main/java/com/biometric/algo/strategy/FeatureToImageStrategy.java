package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.AlgoCommand;
import com.biometric.algo.dto.AlgoRequest;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.socket.AlgoSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 特征对图片比对策略 (Y00.01)
 * 用于将提取好的人脸特征与一组图片进行比对
 */
@Component("FEAT_TO_IMG")
@RequiredArgsConstructor
public class FeatureToImageStrategy extends ComparisonStrategy {

    private final AlgoSocketClient socketClient;

    @Override
    public SocketRecogResult compare(JSONObject data1, JSONObject data2, String version) {
        // 构建基础请求
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.COMPARE_FEAT_TO_IMG) // 对应 Y00.01
                .version(version)
                .build();

        // 填充比对参数
        // PFEATURE1: 特征数据
        request.addParam("PFEATURE1", buildFeatureGroup(data1));
        // PIMAGE2: 图片数据
        request.addParam("PIMAGE2", buildImageGroup(data2));

        // 执行请求
        return socketClient.execute(request, SocketRecogResult.class);
    }
}