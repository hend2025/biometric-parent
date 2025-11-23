package com.biometric.serv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biometric.serv.entity.GrpPsn;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import java.util.List;

public interface GrpPsnMapper extends BaseMapper<GrpPsn> {
    void streamScanAllRelations(@Param("shardIndex") int shardIndex, @Param("totalShards") int totalShards, ResultHandler<GrpPsn> resultHandler);
    
    List<GrpPsn> selectByPsnIds(@Param("psnIds") List<String> psnIds);
}