package com.biometric.serv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biometric.serv.entity.RecognitionLog;
import com.biometric.serv.mapper.RecognitionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 识别日志服务
 */
@Slf4j
@Service
public class RecognitionLogService {

    @Autowired
    private RecognitionLogMapper recognitionLogMapper;

    /**
     * 创建识别日志
     */
    public void createLog(Long userId, String faceId, Double similarity, 
                         Integer recognitionType, Integer recognitionResult,
                         String queryImageUrl, Long costTime) {
        RecognitionLog log = new RecognitionLog();
        log.setUserId(userId);
        log.setFaceId(faceId);
        log.setSimilarity(similarity);
        log.setRecognitionType(recognitionType);
        log.setRecognitionResult(recognitionResult);
        log.setQueryImageUrl(queryImageUrl);
        log.setCostTime(costTime);
        log.setCreateTime(new Date());
        
        recognitionLogMapper.insert(log);
    }

    /**
     * 分页查询识别日志
     */
    public Page<RecognitionLog> listLogs(int pageNum, int pageSize, Long userId, Integer recognitionType) {
        Page<RecognitionLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RecognitionLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(RecognitionLog::getUserId, userId);
        }
        
        if (recognitionType != null) {
            wrapper.eq(RecognitionLog::getRecognitionType, recognitionType);
        }
        
        wrapper.orderByDesc(RecognitionLog::getCreateTime);
        
        return recognitionLogMapper.selectPage(page, wrapper);
    }
}

