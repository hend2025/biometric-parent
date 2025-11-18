package com.biometric.serv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biometric.serv.entity.FaceFtur;
import org.apache.ibatis.session.ResultHandler;

public interface FaceFturMapper extends BaseMapper<FaceFtur> {
    void streamScanAllFeatures(ResultHandler<FaceFtur> resultHandler);
}