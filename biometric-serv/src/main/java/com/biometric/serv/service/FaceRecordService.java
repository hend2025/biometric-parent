package com.biometric.serv.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biometric.serv.entity.FaceRecord;
import com.biometric.serv.mapper.FaceRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 人脸记录服务
 */
@Slf4j
@Service
public class FaceRecordService {

    @Autowired
    private FaceRecordMapper faceRecordMapper;

    /**
     * 创建人脸记录
     */
    public FaceRecord createFaceRecord(String faceId, Long userId, String imageUrl, 
                                       List<Float> featureVector, String remark) {
        FaceRecord record = new FaceRecord();
        record.setFaceId(faceId);
        record.setUserId(userId);
        record.setImageUrl(imageUrl);
        record.setFeatureVector(JSON.toJSONString(featureVector));
        record.setStatus(1);
        record.setRemark(remark);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        
        faceRecordMapper.insert(record);
        log.info("Created face record: id={}, faceId={}, userId={}", record.getId(), faceId, userId);
        return record;
    }

    /**
     * 根据faceId查询
     */
    public FaceRecord getFaceRecordByFaceId(String faceId) {
        LambdaQueryWrapper<FaceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaceRecord::getFaceId, faceId);
        return faceRecordMapper.selectOne(wrapper);
    }

    /**
     * 根据用户ID查询所有人脸记录
     */
    public List<FaceRecord> listFaceRecordsByUserId(Long userId) {
        LambdaQueryWrapper<FaceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaceRecord::getUserId, userId)
               .eq(FaceRecord::getStatus, 1)
               .orderByDesc(FaceRecord::getCreateTime);
        return faceRecordMapper.selectList(wrapper);
    }

    /**
     * 删除人脸记录
     */
    public boolean deleteFaceRecord(String faceId) {
        LambdaQueryWrapper<FaceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaceRecord::getFaceId, faceId);
        int rows = faceRecordMapper.delete(wrapper);
        log.info("Deleted face record: faceId={}, success={}", faceId, rows > 0);
        return rows > 0;
    }

    /**
     * 根据用户ID删除所有人脸记录
     */
    public int deleteFaceRecordsByUserId(Long userId) {
        LambdaQueryWrapper<FaceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaceRecord::getUserId, userId);
        int rows = faceRecordMapper.delete(wrapper);
        log.info("Deleted {} face records for userId={}", rows, userId);
        return rows;
    }
}

