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

    /**
     * 执行算法请求
     * @param request 请求对象
     * @param responseType 响应类型的Class
     * @param <T> 响应泛型
     * @return 解析后的响应对象
     */
    public <T extends SocketResponse<?>> T execute(AlgoRequest request, Class<T> responseType) {
        String funId = request.getCommand().getFunId();
        String jsonRequestStr = request.toTransmissionJson().toJSONString();

        // 使用 try-with-resources 自动关闭 Socket 和 Streams
        try (Socket socket = createSocket();
             OutputStream os = socket.getOutputStream();
             InputStream is = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            // 1. 发送请求 (追加 EOF)
            writer.print(jsonRequestStr + EOF_MARKER);
            writer.flush();

            if (log.isDebugEnabled()) {
                log.debug("算法请求已发送 [FunID={}]: {}...", funId,
                        jsonRequestStr.length() > 100 ? jsonRequestStr.substring(0, 100) : jsonRequestStr);
            }

            // 2. 接收响应
            String responseStr = readResponse(reader);

            // 3. 解析响应
            T result = JSON.parseObject(responseStr, responseType);

            // 4. 基础校验
            if (result == null) {
                throw new AlgoProcessException(-1,"算法引擎返回空响应");
            }
            if (result.getReturnId() != 0) {
                log.warn("算法业务返回错误 [FunID={}]: code={}, desc={}", funId, result.getReturnId(), result.getReturnDesc());
                if(result.getReturnValue()!=null && result.getReturnValue() instanceof String){
                    result.setReturnValue(null);
                    result.setReturnDesc(result.getReturnDesc()+" ## "+result.getReturnValue());
                }
            }

            return result;

        } catch (AlgoProcessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Socket IO异常 [FunID={}]: {}", funId, e.getMessage());
            throw new SocketConnectionException("与算法引擎通信失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("算法处理未知异常 [FunID={}]: {}", funId, e.getMessage(), e);
            throw new AlgoProcessException("算法请求处理过程中发生未知错误", e);
        }
    }

    private Socket createSocket() throws IOException {
        Socket socket = new Socket();
        // 设置连接超时
        socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeout());
        // 设置读取超时
        socket.setSoTimeout(config.getTimeout());
        // 禁用 Nagle 算法，对于这种请求-响应式的小包通信，禁用可减少延迟
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

}