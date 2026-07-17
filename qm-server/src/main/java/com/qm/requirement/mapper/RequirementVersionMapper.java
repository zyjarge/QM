package com.qm.requirement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qm.requirement.entity.RequirementVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RequirementVersionMapper extends BaseMapper<RequirementVersion> {

    int insertVersion(RequirementVersion version);
}
