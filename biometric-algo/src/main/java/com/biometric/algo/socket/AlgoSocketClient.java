package com.biometric.algo.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.AlgoRequest;
import com.biometric.algo.dto.SocketResponse;
import com.biometric.algo.exception.AlgoProcessException;
import com.biometric.algo.exception.SocketConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AlgoSocketClient {

    private static final String EOF_MARKER = "<EOF>";
    private final AlgoSocketConfig config;

    public AlgoSocketClient(AlgoSocketConfig config) {
        this.config = config;
    }

    /**
     * 统一执行入口
     */
    public <T extends SocketResponse<?>> T execute(AlgoRequest request, Class<T> responseType) {
        String funId = request.getCommand().getFunId();

        // 使用 try-with-resources 确保 Socket 即使在异常时也能关闭
        try (Socket socket = createSocket();
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. 发送请求
            String jsonRequest = request.toTransmissionJson().toJSONString();
            out.print(jsonRequest + EOF_MARKER);
            out.flush();

            log.debug("算法请求发送成功 [FunID={}]", funId);

            // 2. 接收响应
            String responseStr = readResponse(in);

            // 3. 解析响应
            T result = JSON.parseObject(responseStr, responseType);

            // 4. 业务层面的错误检查 (ReturnId != 0)
            if (result != null && result.getReturnId() != 0) {
                log.warn("算法返回错误 [FunID={}]: code={}, desc={}", funId, result.getReturnId(), result.getReturnDesc());
                // 这里可以选择抛出异常，或者让上层处理
            }
            return result;

        } catch (IOException e) {
            log.error("算法Socket通信IO异常 [FunID={}]: {}", funId, e.getMessage());
            throw new SocketConnectionException("算法引擎连接失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("算法请求处理未知异常 [FunID={}]: {}", funId, e.getMessage(), e);
            throw new AlgoProcessException("算法请求处理失败", e);
        }
    }

    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeout());
        socket.setSoTimeout(config.getTimeout());
        socket.setTcpNoDelay(true); // 禁用 Nagle 算法，减少延迟
        return socket;
    }

    private String readResponse(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        String response = sb.toString();
        if (response.endsWith(EOF_MARKER)) {
            return response.substring(0, response.length() - EOF_MARKER.length());
        }
        return response;
    }
    
}