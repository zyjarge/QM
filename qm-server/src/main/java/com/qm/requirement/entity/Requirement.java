package com.qm.requirement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirements")
public class Requirement extends BaseEntity {

    private String reqNo;           // REQ-2026-0001
    private String title;
    private String reqType;         // feature/optimization/bug/data/api
    private String productLine;
    private String module;
    private String priority;        // P0/P1/P2/P3
    private String status;          // draft/clarifying/.../archived
    private String currentVersionId;
    private String ownerId;         // PM 负责人
    private Boolean isConfidential;
    private String sourceChannel;   // web/feishu/wecom/ai
    private String createdBy;
    private LocalDateTime closedAt;
}
