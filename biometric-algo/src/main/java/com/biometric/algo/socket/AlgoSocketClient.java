package com.biometric.algo.socket;

import com.alibaba.fastjson.JSON;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.AlgoRequest;
import com.biometric.algo.dto.SocketResponse;
import com.biometric.algo.exception.AlgoProcessException;
import com.biometric.algo.exception.SocketConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 算法Socket客户端
 * 负责与生物识别算法引擎进行Socket通信，封装请求发送和响应解析逻辑
 * 
 * 主要功能:
 * - 建立Socket连接并发送算法请求
 * - 接收并解析算法引擎的响应
 * - 统一处理通信异常和响应错误
 * 
 * @author biometric-algo
 * @version 1.0
 * @see AlgoRequest
 * @see SocketResponse
 */
@Slf4j
@Component
public class AlgoSocketClient {

    /** 消息结束标记 */
    private static final String EOF_MARKER = "<EOF>";
    
    /** 算法配置 */
    @Autowired
    private AlgoSocketConfig config;

    /**
     * 构造函数
     * 
     * @param config 算法配置
     */
    public AlgoSocketClient(AlgoSocketConfig config) {
        this.config = config;
    }

    /**
     * 执行算法请求
     * 
     * @param request 算法请求
     * @param responseType 响应类型
     * @return 算法响应
     */
    public <T extends SocketResponse<?>> T execute(AlgoRequest request, Class<T> responseType) {

        try (Socket socket = createSocket();
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String jsonRequest = request.toTransmissionJson().toJSONString();
            out.print(jsonRequest + EOF_MARKER);
            out.flush();

            String response = readResponse(in);

            T result = JSON.parseObject(response, responseType);

            if (result.getReturnId() != 0) {
                if(result.getReturnValue() != null && result.getReturnValue() instanceof String){
                    result.setReturnValue(null);
                    result.setReturnDesc(result.getReturnDesc() + ": " + result.getReturnValue());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("算法Socket执行错误 [命令={}]: {}", request.getCommand(), e.getMessage());
            try {
                T errorResult = responseType.newInstance();
                errorResult.setReturnId(-1);
                errorResult.setReturnDesc("解析响应失败: " + e.getMessage());
                return errorResult;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new AlgoProcessException("无法创建错误结果对象", ex);
            }
        }

    }

    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeout());
        socket.setSoTimeout(config.getTimeout());
        return socket;
    }

    private String readResponse(BufferedReader in) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            responseBuilder.append(line);
        }

        String response = responseBuilder.toString();
        if (response.endsWith(EOF_MARKER)) {
            return response.substring(0, response.length() - EOF_MARKER.length());
        }
        return response;
    }

}