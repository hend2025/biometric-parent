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
 * Socket client with improved resource management
 * Applies Try-with-resources pattern for automatic resource cleanup
 */
@Slf4j
@Component
public class SocketClient {
    
    private final AlgoSocketConfig config;
    private static final String EOF_MARKER = "<EOF>";
    
    public SocketClient(AlgoSocketConfig config) {
        this.config = config;
    }
    
    /**
     * Send request to algorithm engine
     * Uses try-with-resources for automatic resource management
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
            
            log.debug("Sent request to algo engine [FunID={}]", funId);
            
            // Receive response
            String response = readResponse(in);
            
            log.debug("Received response from algo engine [FunID={}]", funId);
            return response;
            
        } catch (IOException e) {
            log.error("Socket communication error [FunID={}]: {}", funId, e.getMessage());
            throw new SocketConnectionException(
                    "Failed to communicate with biometric algo engine", e);
        }
    }
    
    /**
     * Create and configure socket connection
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
     * Read response from input stream
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
