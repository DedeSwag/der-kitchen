package com.der.kitchen.file.config;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
@Validated
public class MinioProperties {

    @NotBlank
    private String endpoint;
    @NotBlank
    private String accessKey;
    @NotBlank
    private String secretKey;
    @NotBlank
    private String bucket;

    /** 预签名 URL 有效期（秒），默认 1 小时 */
    private int presignedExpirySeconds = 3600;
}
