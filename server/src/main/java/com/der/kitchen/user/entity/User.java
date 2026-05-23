package com.der.kitchen.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    private String openid;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
}
