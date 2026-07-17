package com.qm.requirement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_versions")
public class RequirementVersion extends BaseEntity {

    private String requirementId;
    private Integer versionNo;
    private String content;         // jsonb 块文档
    private String contentText;     // 纯文本用于检索
    private String contentHash;     // SHA-256
    private String fieldsData;      // jsonb 模板字段值
    private String editedBy;
    private String changeSummary;
}
