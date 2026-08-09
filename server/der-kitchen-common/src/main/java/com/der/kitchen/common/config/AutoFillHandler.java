package com.der.kitchen.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段自动填充
 */
@Slf4j
@Component
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        String currentUser = getCurrentUsername();
        this.strictInsertFill(metaObject, "createBy", String.class, currentUser);
        this.strictInsertFill(metaObject, "updateBy", String.class, currentUser);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", this::getCurrentUsername, String.class);
    }

    private String getCurrentUsername() {
        try {
            if (!StpUtil.isLogin()) {
                return "system";
            }
            Object username = StpUtil.getSession().get("username");
            return username != null && !username.toString().isBlank()
                    ? username.toString()
                    : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
