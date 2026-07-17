package com.qm.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_votes")
public class ReviewVote extends BaseEntity {

    private String flowId;
    private String voterId;
    private String decision;        // approve/reject/abstain/pending
    private String comment;
    private LocalDateTime votedAt;
    private String delegatedTo;
    private String cardMsgId;
}
