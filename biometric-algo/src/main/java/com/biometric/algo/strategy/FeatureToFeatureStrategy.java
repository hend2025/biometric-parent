package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.AlgoCommand;
import com.biometric.algo.dto.AlgoRequest;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.socket.AlgoSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("FEAT_TO_FEAT")
@RequiredArgsConstructor
public class FeatureToFeatureStrategy extends ComparisonStrategy {

    private final AlgoSocketClient socketClient;
    private AlgoRequest AlgoRequestBuilder;

    @Override
    public SocketRecogResult compare(JSONObject data1, JSONObject data2, String version) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.COMPARE_FEAT_TO_FEAT)
                .version(version)
                .build();

        // 使用 Builder 的辅助方法构建参数
        request.addParam("PFEATURE1", buildFeatureGroup(data1));
        request.addParam("PFEATURE2", buildFeatureGroup(data2));

        return socketClient.execute(request, SocketRecogResult.class);
    }
}