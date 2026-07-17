package com.qm.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("baselines")
public class Baseline extends BaseEntity {

    private String requirementId;
    private String versionId;
    private String contentHash;
    private String snapshot;
    private String signedBy;
    private LocalDateTime signedAt;
    private String signatureMeta;
}
