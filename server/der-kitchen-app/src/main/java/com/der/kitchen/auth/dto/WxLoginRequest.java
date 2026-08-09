package com.der.kitchen.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WxLoginRequest {
    @NotBlank(message = "code不能为空")
    @Size(max = 128, message = "code长度不合法")
    private String code;

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;
    @Size(max = 500, message = "头像地址不能超过500个字符")
    private String avatarUrl;
}
