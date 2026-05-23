package com.der.kitchen.common.util;

import com.der.kitchen.common.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AppConfig appConfig;

    private SecretKey getSigningKey() {
        String secret = appConfig.getAuth().getJwtSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Token
     */
    public String generateToken(Long userId, String role, String nickname) {
        int expireDays = appConfig.getAuth().getJwtExpireDays();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) expireDays * 24 * 60 * 60 * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("nickname", nickname)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token，返回 Claims；无效时返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Claims 构建 UserContext
     */
    public UserContext toUserContext(Claims claims) {
        UserContext ctx = new UserContext();
        ctx.setUserId(Long.parseLong(claims.getSubject()));
        ctx.setRole(claims.get("role", String.class));
        ctx.setNickname(claims.get("nickname", String.class));
        return ctx;
    }
}
