package com.der.kitchen.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String role;
    private String nickname;
    private String avatarUrl;
}
