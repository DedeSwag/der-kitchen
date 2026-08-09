package com.der.kitchen.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.der.kitchen.auth.dto.AdminLoginRequest;
import com.der.kitchen.auth.dto.LoginResponse;
import com.der.kitchen.auth.dto.WxLoginRequest;
import com.der.kitchen.auth.util.WxUtil;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.config.AppConfig;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WxUtil wxUtil;
    private final UserService userService;
    private final AppConfig appConfig;

    @Operation(summary = "微信登录")
    @SecurityRequirements
    @PostMapping("/wx-login")
    public R<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest req) {
        String openid = wxUtil.code2Openid(req.getCode());

        List<String> allowedOpenids = appConfig.getAuth().getAllowedOpenids();
        if (allowedOpenids != null && !allowedOpenids.isEmpty() && !allowedOpenids.contains(openid)) {
            throw new BizException(403, "未授权用户，请联系管理员");
        }

        List<String> adminOpenids = appConfig.getAuth().getAdminOpenids();
        String expectedRole = adminOpenids != null && adminOpenids.contains(openid) ? "admin" : "user";
        User user = userService.getByOpenid(openid);
        if (user == null) {
            user = userService.createUser(
                    openid,
                    req.getNickname() != null ? req.getNickname() : "用户",
                    req.getAvatarUrl(),
                    expectedRole);
            log.info("新用户注册完成: userId={}, role={}", user.getId(), expectedRole);
        } else {
            if (!"active".equals(user.getStatus())) {
                throw new BizException(403, "账号已被禁用");
            }
            boolean changed = false;
            if (req.getNickname() != null) {
                user.setNickname(req.getNickname());
                user.setAvatarUrl(req.getAvatarUrl());
                changed = true;
            }
            if (!expectedRole.equals(user.getRole())) {
                user.setRole(expectedRole);
                changed = true;
            }
            if (changed) {
                userService.updateUser(user);
            }
        }

        return login(user);
    }

    @Operation(summary = "管理端账号密码登录")
    @SecurityRequirements
    @PostMapping("/admin-login")
    public R<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        User user = userService.getByUsername(req.getUsername());
        if (user == null || user.getPassword() == null
                || !BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!"admin".equals(user.getRole())) {
            throw new BizException(403, "无管理员权限");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BizException(403, "账号已被禁用");
        }

        return login(user);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "修改密码（管理端）")
    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        Long userId = SecurityUtils.requireUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(401, "登录用户不存在");
        }
        if (user.getPassword() != null && !BCrypt.checkpw(req.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }
        user.setPassword(BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt()));
        userService.updateUser(user);
        StpUtil.kickout(userId);
        return R.ok();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public R<LoginResponse> me() {
        User user = userService.getById(SecurityUtils.requireUserId());
        if (user == null) {
            throw new BizException(401, "登录用户不存在");
        }
        return R.ok(toLoginResponse(user, null));
    }

    private R<LoginResponse> login(User user) {
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", resolveSessionUsername(user));
        return R.ok(toLoginResponse(user, StpUtil.getTokenValue()));
    }

    private String resolveSessionUsername(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getId().toString();
    }

    private LoginResponse toLoginResponse(User user, String token) {
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Data
    static class ChangePasswordRequest {
        @NotBlank
        @Size(max = 72, message = "密码不能超过72位")
        private String oldPassword;

        @NotBlank
        @Size(min = 6, max = 72, message = "密码长度应为6到72位")
        private String newPassword;
    }
}
