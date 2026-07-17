package com.qm.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trace_links")
public class TraceLink extends BaseEntity {

    private String requirementId;
    private String linkType;        // task/commit/branch/test_case/release/mr
    private String externalId;
    private String externalUrl;
    private String title;
    private String source;          // manual/webhook
}
