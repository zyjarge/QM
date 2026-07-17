package com.qm.requirement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_stakeholders")
public class RequirementStakeholder extends BaseEntity {

    private String requirementId;
    private String userId;
    private String stakeholderRole; // requester/biz_owner/pm/dev_lead/qa/watcher
    private String addedBy;
    private LocalDateTime addedAt;
}
