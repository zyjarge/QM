package com.qm.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("templates")
public class Template extends BaseEntity {

    private String name;
    private String reqType;         // feature/optimization/bug/data/api
    private String fieldSchema;     // jsonb 动态表单定义
    private Boolean isActive;
}
