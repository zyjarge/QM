package com.qm.archiver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_archives")
public class MessageArchive {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requirementId;
    private String imMsgId;
    private String imProvider;
    private String senderId;
    private String msgType;         // text/card/file/image/audio/video
    private String content;         // jsonb 原文快照
    private String contentText;
    private Boolean isKeyInfo;
    private Boolean keyInfoMerged;
    private LocalDateTime msgTime;
    private LocalDateTime createdAt;
}
