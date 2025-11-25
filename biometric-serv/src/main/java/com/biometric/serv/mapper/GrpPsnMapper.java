package com.biometric.serv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.biometric.serv.entity.GrpPsn;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import java.util.Collections;
import java.util.List;

public interface GrpPsnMapper extends BaseMapper<GrpPsn> {
    void streamScanAllRelations(@Param("shardIndex") int shardIndex, @Param("totalShards") int totalShards, ResultHandler<GrpPsn> resultHandler);

    default List<GrpPsn> selectByPsnIds(List<String> psnIds) {
        if (psnIds == null || psnIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(Wrappers.<GrpPsn>lambdaQuery()
                .select(GrpPsn::getPsnTmplNo, GrpPsn::getGrpId)
                .in(GrpPsn::getPsnTmplNo, psnIds));
    }

}