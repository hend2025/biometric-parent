package com.biometric.serv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.entity.GrpPsn;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import java.util.Collections;
import java.util.List;

public interface FaceFturMapper extends BaseMapper<FaceFtur> {
    void streamScanAllFeatures(@Param("shardIndex") int shardIndex, @Param("totalShards") int totalShards, ResultHandler<FaceFtur> resultHandler);

    default List<FaceFtur> selectByPsnIds(List<String> psnIds) {
        if (psnIds == null || psnIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(Wrappers.<FaceFtur>query()
                .eq("VALI_FLAG", "1")
                .in("PSN_TMPL_NO", psnIds));
    }

}