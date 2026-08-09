package com.der.kitchen.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    @JsonIgnore
    private String openid;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
}
