package com.biometric.algo.socket;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.exception.SocketConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Socket客户端（改进版）
 * 使用Try-with-resources模式进行自动资源管理
 * 
 * 改进点：
 * - 自动关闭Socket、输入输出流等资源
 * - 统一异常处理机制
 * - 标准化的通信协议（EOF标记）
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Slf4j
@Component
public class ClaudeSocketClient {
    
    /** 消息结束标记 */
    private static final String EOF_MARKER = "<EOF>";
    
    /** 算法配置 */
    private AlgoSocketConfig config;

    /**
     * 构造函数
     * 
     * @param config 算法配置对象
     */
    public ClaudeSocketClient(AlgoSocketConfig config) {
        this.config = config;
    }
    
    /**
     * 发送请求到算法引擎
     * 使用try-with-resources模式进行自动资源管理
     * 
     * @param params 请求参数
     * @return 算法引擎的响应字符串
     * @throws SocketConnectionException 当Socket通信失败时
     */
    public String sendRequest(JSONObject params) {
        String funId = params.getString("FUNID");
        
        try (Socket socket = createSocket();
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Send request
            String jsonRequest = JSONObject.toJSONString(params);
            String payload = jsonRequest + EOF_MARKER;
            out.print(payload);
            out.flush();
            
            log.debug("发送请求到算法引擎 [FunID={}]", funId);
            
            // Receive response
            String response = readResponse(in);
            
            log.debug("收到算法引擎响应 [FunID={}]", funId);
            return response;
            
        } catch (IOException e) {
            log.error("Socket通信错误 [FunID={}]: {}", funId, e.getMessage());
            throw new SocketConnectionException(
                    "与生物识别算法引擎通信失败", e);
        }
    }
    
    /**
     * 创建并配置Socket连接
     * 
     * @return 配置好的Socket对象
     * @throws IOException 当连接创建失败时
     */
    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        socket.connect(
                new InetSocketAddress(config.getHost(), config.getPort()), 
                config.getTimeout());
        socket.setSoTimeout(config.getTimeout());
        return socket;
    }
    
    /**
     * 从输入流读取响应
     * 
     * @param in 输入流
     * @return 响应字符串（不包含EOF标记）
     * @throws IOException 当读取失败时
     */
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
