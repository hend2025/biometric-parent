package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.AlgoCommand;
import com.biometric.algo.dto.AlgoRequest;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.socket.AlgoSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 图片对图片比对策略 (Y00.02)
 * 用于两组图片之间的直接比对（后台会自动提取特征并比对）
 */
@Component("IMG_TO_IMG")
@RequiredArgsConstructor
public class ImageToImageStrategy extends ComparisonStrategy {

    private final AlgoSocketClient socketClient;

    @Override
    public SocketRecogResult compare(JSONObject data1, JSONObject data2, String version) {
        // 构建基础请求
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.COMPARE_IMG_TO_IMG) // 对应 Y00.02
                .version(version)
                .build();

        // 填充比对参数
        // PIMAGE1: 第一组图片
        request.addParam("PIMAGE1", buildImageGroup(data1));
        // PIMAGE2: 第二组图片
        request.addParam("PIMAGE2", buildImageGroup(data2));

        // 执行请求
        return socketClient.execute(request, SocketRecogResult.class);
    }
}