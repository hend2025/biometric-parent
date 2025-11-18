package com.biometric.serv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biometric.serv.entity.GrpPsn;
import org.apache.ibatis.session.ResultHandler;

public interface GrpPsnMapper extends BaseMapper<GrpPsn> {
    void streamScanAllRelations(ResultHandler<GrpPsn> handler);
}