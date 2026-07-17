package com.qm.requirement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qm.requirement.entity.Requirement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequirementMapper extends BaseMapper<Requirement> {

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(req_no FROM 11) AS INTEGER)), 0) + 1 FROM requirements WHERE req_no LIKE 'REQ-' || EXTRACT(YEAR FROM NOW()) || '-%'")
    Long selectReqNoSeq();
}
