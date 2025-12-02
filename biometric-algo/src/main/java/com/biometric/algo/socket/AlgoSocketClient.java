package com.biometric.algo.socket;

import com.alibaba.fastjson.JSON;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.AlgoRequest;
import com.biometric.algo.dto.SocketResponse;
import com.biometric.algo.exception.AlgoProcessException;
import com.biometric.algo.exception.SocketConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 算法引擎 Socket 客户端
 * 负责底层 TCP 通信、协议封装与响应解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlgoSocketClient {

    private static final String EOF_MARKER = "<EOF>";
    private final AlgoSocketConfig config;

    public <T extends SocketResponse<?>> T execute(AlgoRequest request, Class<T> responseType) {
        String funId = request.getCommand().getFunId();
        String jsonRequestStr = request.toTransmissionJson().toJSONString();

        // 使用 try-with-resources 自动关闭 Socket 和 Streams
        try (Socket socket = createSocket();
             OutputStream os = socket.getOutputStream();
             InputStream is = socket.getInputStream();
             // 使用 UTF-8 编码
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            // 1. 发送请求
            writer.print(jsonRequestStr + EOF_MARKER);
            writer.flush();

            if (log.isDebugEnabled()) {
                log.debug("请求已发送 [FunID={}]: {}", funId,
                        jsonRequestStr.length() > 200 ? jsonRequestStr.substring(0, 200) + "..." : jsonRequestStr);
            }

            // 2. 接收响应
            String responseStr = readResponse(reader);

            // 3. 解析响应
            T result = JSON.parseObject(responseStr, responseType);

            // 4. 校验结果
            validateResult(result, funId);

            return result;

        } catch (SocketConnectionException | AlgoProcessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Socket IO异常 [FunID={}]: {}", funId, e.getMessage());
            throw new SocketConnectionException("与算法引擎通信失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("算法处理未知异常 [FunID={}]: {}", funId, e.getMessage(), e);
            throw new AlgoProcessException("算法请求处理过程中发生未知错误", e);
        }
    }

    /**
     * 创建新的 Socket 连接
     */
    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        // 建立连接
        socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeout());
        // 设置读取超时
        socket.setSoTimeout(config.getTimeout());
        // 禁用 Nagle 算法，减少延迟
        socket.setTcpNoDelay(true);
        return socket;
    }

    private String readResponse(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder(1024);
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String response = sb.toString();

        // 去除 EOF 标记
        if (response.endsWith(EOF_MARKER)) {
            return response.substring(0, response.length() - EOF_MARKER.length());
        }
        return response;
    }

    private void validateResult(SocketResponse<?> result, String funId) {
        if (result == null) {
            throw new AlgoProcessException(-1,"算法引擎返回空响应（可能是网络连接中断）");
        }
        if (result.getReturnId() != 0) {
            log.warn("算法业务返回错误 [FunID={}]: code={}, desc={}", funId, result.getReturnId(), result.getReturnDesc());
        }
    }
}