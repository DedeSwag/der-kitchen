package com.der.kitchen.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.der.kitchen.common.result.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<?>> handleNotLogin(NotLoginException e) {
        return fail(HttpStatus.UNAUTHORIZED, "登录状态无效或已过期");
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<R<?>> handleForbidden(RuntimeException e) {
        return fail(HttpStatus.FORBIDDEN, "无权访问该资源");
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<?>> handleBiz(BizException e) {
        HttpStatus status = resolveStatus(e.getCode());
        log.warn("业务请求失败: status={}, type={}", status.value(), e.getClass().getSimpleName());
        return fail(status, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<?>> handleValidation(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        return fail(HttpStatus.BAD_REQUEST, message.isBlank() ? "请求参数不合法" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<?>> handleConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        return fail(HttpStatus.BAD_REQUEST, message.isBlank() ? "请求参数不合法" : message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<R<?>> handleBadRequest(Exception e) {
        return fail(HttpStatus.BAD_REQUEST, "请求参数格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return fail(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<?>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return fail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请求内容类型不支持");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<?>> handleNotFound(NoResourceFoundException e) {
        return fail(HttpStatus.NOT_FOUND, "请求资源不存在");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<?>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return fail(HttpStatus.PAYLOAD_TOO_LARGE, "上传文件过大");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<R<?>> handleDataConflict(DataIntegrityViolationException e) {
        log.warn("数据库约束冲突: {}", e.getClass().getSimpleName());
        return fail(HttpStatus.CONFLICT, "数据冲突，请检查重复项或关联关系");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleException(Exception e) {
        log.error("未处理的系统异常: type={}", e.getClass().getName(), e);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, "系统异常，请稍后重试");
    }

    private HttpStatus resolveStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null && status.isError() ? status : HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<R<?>> fail(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(R.fail(status.value(), message));
    }
}
