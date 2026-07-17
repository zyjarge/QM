package com.qm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qm.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    private String name;
    private String email;
    private String feishuOpenId;
    private String wecomUserId;
    private String role;        // REQUESTER/PM/REVIEWER/ADMIN
    private String status;      // active/inactive
}
