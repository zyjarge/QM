package com.qm.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_flows")
public class ReviewFlow extends BaseEntity {

    private String requirementId;
    private Integer roundNo;
    private String reviewType;      // tech/biz/final
    private String mode;            // all(会签) / any(或签)
    private String status;          // in_progress/passed/rejected/cancelled
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
