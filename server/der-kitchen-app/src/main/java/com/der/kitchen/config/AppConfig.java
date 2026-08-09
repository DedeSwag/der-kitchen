package com.der.kitchen.config;

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
    private Cors cors = new Cors();

    @Data
    public static class Wx {
        private String appId;
        private String appSecret;
        private Subscribe subscribe = new Subscribe();
    }

    @Data
    public static class Subscribe {
        private boolean enabled;
        private String templateId;
        private String page = "pages/order/detail?id={orderId}";
        private String mealKey = "thing1";
        private String summaryKey = "thing2";
        private String timeKey = "time3";
        private int maxAttempts = 5;
        private long retryBaseSeconds = 30;
        private long processingTimeoutSeconds = 300;
        private long workerDelayMillis = 3000;
    }

    @Data
    public static class Auth {
        private List<String> allowedOpenids;
        private List<String> adminOpenids;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }
}
