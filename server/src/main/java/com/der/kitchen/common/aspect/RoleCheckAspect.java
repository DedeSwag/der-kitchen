package com.der.kitchen.common.aspect;

import com.der.kitchen.common.annotation.RequireRole;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.util.UserContextHolder;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 角色校验切面
 */
@Aspect
@Component
public class RoleCheckAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        String currentRole = UserContextHolder.getRole();
        if (currentRole == null || !currentRole.equals(requireRole.value())) {
            throw new BizException(403, "无权限访问");
        }
    }

    @Before("@within(requireRole)")
    public void checkClassRole(RequireRole requireRole) {
        String currentRole = UserContextHolder.getRole();
        if (currentRole == null || !currentRole.equals(requireRole.value())) {
            throw new BizException(403, "无权限访问");
        }
    }
}
