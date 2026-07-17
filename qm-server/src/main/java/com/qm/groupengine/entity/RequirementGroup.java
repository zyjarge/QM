package com.qm.groupengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_groups")
public class RequirementGroup extends BaseEntity {

    private String requirementId;
    private String imProvider;      // feishu/wecom
    private String chatId;
    private String groupName;
    private String status;          // active/dissolving/dissolved
    private LocalDateTime dissolvedAt;
    private Boolean archiveExported;
    private String archivePath;
}
