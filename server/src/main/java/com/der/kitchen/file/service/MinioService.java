package com.der.kitchen.file.service;

import com.der.kitchen.common.config.MinioConfig;
import com.der.kitchen.common.exception.BizException;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long COMPRESS_THRESHOLD = 500 * 1024; // 500KB

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
                log.info("Created MinIO bucket: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("MinIO 初始化失败", e);
        }
    }

    public String upload(MultipartFile file) {
        // 校验大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件大小不能超过5MB");
        }

        // 校验后缀
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BizException("仅支持 jpg/png/webp 格式");
        }

        try {
            InputStream inputStream;
            long size;
            String contentType = file.getContentType();

            // 超过500KB自动压缩
            if (file.getSize() > COMPRESS_THRESHOLD && !"webp".equals(ext)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .scale(1.0)
                        .outputQuality(0.7)
                        .toOutputStream(out);
                byte[] compressed = out.toByteArray();
                inputStream = new ByteArrayInputStream(compressed);
                size = compressed.length;
                log.info("图片压缩: {}KB -> {}KB", file.getSize() / 1024, size / 1024);
            } else {
                inputStream = file.getInputStream();
                size = file.getSize();
            }

            // 生成唯一文件名
            String objectName = "dishes/" + UUID.randomUUID() + "." + ext;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            // 返回访问URL
            return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BizException("文件上传失败");
        }
    }

    public void delete(String url) {
        try {
            String prefix = minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/";
            if (!url.startsWith(prefix)) return;
            String objectName = url.substring(prefix.length());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("文件删除失败: {}", url, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
