package com.der.kitchen.user.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.UserContextHolder;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<User> info() {
        Long userId = UserContextHolder.getUserId();
        User user = userService.getById(userId);
        return R.ok(user);
    }
}
