package com.qm.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_units")
public class OrgUnit extends BaseEntity {

    private String name;
    private String parentId;
    private String imDeptId;
}
