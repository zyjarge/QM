package com.qm.notify.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notifications")
public class Notification extends BaseEntity {

    private String userId;
    private String type;            // review_request/baseline_sign/timeout/...
    private String requirementId;
    private String payload;         // jsonb
    private String channel;         // im_card/im_msg/web
    private String status;          // pending/sent/read
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
