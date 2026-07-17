package com.qm.requirement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acceptance_criteria")
public class AcceptanceCriteria extends BaseEntity {

    private String requirementId;
    private String versionId;
    private String criterionType;   // given_when_then / checklist
    private String content;
    private Integer sortOrder;
}
