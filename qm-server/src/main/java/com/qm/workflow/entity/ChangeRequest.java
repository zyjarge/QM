package com.qm.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("change_requests")
public class ChangeRequest extends BaseEntity {

    private String requirementId;
    private String fromVersionId;
    private String toVersionId;
    private String diff;            // jsonb
    private String reason;
    private String impactAssessment;
    private String level;           // minor/major
    private String status;          // pending/approved/rejected/merged
    private String approvedBy;
    private LocalDateTime approvedAt;
}
