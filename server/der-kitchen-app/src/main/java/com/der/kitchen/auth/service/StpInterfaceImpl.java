package com.der.kitchen.auth.service;

import cn.dev33.satoken.stp.StpInterface;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 角色数据源。角色始终从数据库读取，避免把可变权限固化在 token 中。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userService.getById(Long.valueOf(String.valueOf(loginId)));
        if (user == null || !"active".equals(user.getStatus()) || user.getRole() == null) {
            return List.of();
        }
        return List.of(user.getRole());
    }
}
