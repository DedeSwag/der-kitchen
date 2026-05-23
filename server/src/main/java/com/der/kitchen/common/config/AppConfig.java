package com.der.kitchen.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Wx wx = new Wx();
    private Auth auth = new Auth();

    @Data
    public static class Wx {
        private String appId;
        private String appSecret;
    }

    @Data
    public static class Auth {
        private List<String> allowedOpenids;
        private String jwtSecret;
        private int jwtExpireDays = 7;
    }
}
