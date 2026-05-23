package com.der.kitchen.auth.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.der.kitchen.auth.dto.AdminLoginRequest;
import com.der.kitchen.auth.dto.LoginResponse;
import com.der.kitchen.auth.dto.WxLoginRequest;
import com.der.kitchen.common.config.AppConfig;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.JwtUtil;
import com.der.kitchen.common.util.UserContextHolder;
import com.der.kitchen.common.util.WxUtil;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WxUtil wxUtil;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AppConfig appConfig;

    @Operation(summary = "微信登录")
    @PostMapping("/wx-login")
    public R<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest req) {
        // 1. 通过 code 换取 openid
        String openid = wxUtil.code2Openid(req.getCode());

        // 2. 校验白名单
        List<String> allowed = appConfig.getAuth().getAllowedOpenids();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(openid)) {
            throw new BizException(403, "未授权用户，请联系管理员");
        }

        // 3. 查询或创建用户
        User user = userService.getByOpenid(openid);
        if (user == null) {
            // 首个注册的为 admin，其余为 user
            String role = (allowed != null && !allowed.isEmpty() && allowed.get(0).equals(openid))
                    ? "admin" : "user";
            user = userService.createUser(openid,
                    req.getNickname() != null ? req.getNickname() : "用户",
                    req.getAvatarUrl(),
                    role);
            log.info("新用户注册: openid={}, role={}", openid, role);
        } else {
            // 更新昵称头像
            if (req.getNickname() != null) {
                user.setNickname(req.getNickname());
                user.setAvatarUrl(req.getAvatarUrl());
                userService.updateUser(user);
            }
        }

        // 4. 签发 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getNickname());

        return R.ok(LoginResponse.builder()
                .token(token)
                .role(user.getRole())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }

    @Operation(summary = "管理端账密登录（Web）")
    @PostMapping("/admin-login")
    public R<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        User user = userService.getByUsername(req.getUsername());
        if (user == null || user.getPassword() == null) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!"admin".equals(user.getRole())) {
            throw new BizException(403, "无管理员权限");
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BizException(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getNickname());
        return R.ok(LoginResponse.builder()
                .token(token)
                .role(user.getRole())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }

    @Operation(summary = "修改密码（管理端）")
    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        Long userId = UserContextHolder.getUserId();
        User user = userService.getById(userId);
        if (user.getPassword() != null && !BCrypt.checkpw(req.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }
        user.setPassword(BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt()));
        userService.updateUser(user);
        return R.ok(null);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public R<LoginResponse> me() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) throw new BizException(401, "未登录");
        User user = userService.getById(userId);
        return R.ok(LoginResponse.builder()
                .role(user.getRole())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }

    // inner DTO
    @lombok.Data
    static class ChangePasswordRequest {
        @jakarta.validation.constraints.NotBlank
        private String oldPassword;
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 6, message = "密码不能少于6位")
        private String newPassword;
    }
}
