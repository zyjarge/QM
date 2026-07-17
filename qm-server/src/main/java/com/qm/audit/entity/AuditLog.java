package com.qm.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String actorId;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;          // jsonb
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
