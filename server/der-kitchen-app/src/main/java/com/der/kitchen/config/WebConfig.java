package com.der.kitchen.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserService userService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        return;
                    }

                    SaRouter.match("/api/**")
                            .notMatch("/api/v1/auth/wx-login", "/api/v1/auth/admin-login")
                            .check(r -> {
                                StpUtil.checkLogin();
                                Long userId = SecurityUtils.requireUserId();
                                User user = userService.getById(userId);
                                if (user == null || !"active".equals(user.getStatus())) {
                                    StpUtil.logout();
                                    throw new BizException(403, "账号不存在或已被禁用");
                                }
                            });

                    SaRouter.match("/api/v1/admin/**", r -> StpUtil.checkRole("admin"));
                    SaRouter.match("/api/common/file/**", r -> StpUtil.checkRole("admin"));
                }))
                .addPathPatterns("/**");
    }
}
