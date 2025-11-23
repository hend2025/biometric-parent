package com.biometric.serv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biometric.serv.entity.PsnTmpl;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

public interface PsnTmplMapper extends BaseMapper<PsnTmpl> {
    void streamScanPsnTmpls(@Param("shardIndex") int shardIndex, @Param("totalShards") int totalShards, ResultHandler<PsnTmpl> resultHandler);
}
