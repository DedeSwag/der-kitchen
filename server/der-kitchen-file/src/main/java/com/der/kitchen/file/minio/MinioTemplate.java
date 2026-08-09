package com.der.kitchen.file.minio;

import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.file.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MinIO 常用操作封装，供 FileService 调用
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioTemplate {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final Set<String> readyBuckets = ConcurrentHashMap.newKeySet();

    /**
     * 上传对象
     *
     * @param bucket      桶名，null 时使用配置默认桶
     * @param objectKey   对象键（路径）
     * @param stream      数据流
     * @param size        字节长度
     * @param contentType MIME 类型
     */
    public void upload(String bucket, String objectKey, InputStream stream, long size, String contentType) {
        String b = bucket != null ? bucket : properties.getBucket();
        try {
            ensureBucket(b);
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(b)
                    .object(objectKey)
                    .stream(stream, size, -1);
            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }
            minioClient.putObject(builder.build());
        } catch (Exception e) {
            log.error("对象存储上传失败: bucket={}, key={}, type={}", b, objectKey,
                    e.getClass().getSimpleName(), e);
            throw new BizException(502, "文件存储服务异常");
        }
    }

    /**
     * 生成临时访问 URL（GET 方式预签名）
     */
    public String getPresignedUrl(String bucket, String objectKey) {
        String b = bucket != null ? bucket : properties.getBucket();
        int expiry = Math.max(60, properties.getPresignedExpirySeconds());
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(b)
                            .object(objectKey)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("对象存储生成访问地址失败: bucket={}, key={}, type={}", b, objectKey,
                    e.getClass().getSimpleName(), e);
            throw new BizException(502, "文件存储服务异常");
        }
    }

    /**
     * 删除对象（失败时重试一次）
     */
    public void delete(String bucket, String objectKey) {
        String b = bucket != null ? bucket : properties.getBucket();
        try {
            removeObject(b, objectKey);
        } catch (Exception first) {
            try {
                Thread.sleep(100L);
                removeObject(b, objectKey);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                log.error("对象存储删除被中断: bucket={}, key={}", b, objectKey, interrupted);
                throw new BizException(502, "文件存储服务异常");
            } catch (Exception retry) {
                log.error("对象存储删除失败: bucket={}, key={}, type={}", b, objectKey,
                        retry.getClass().getSimpleName(), retry);
                throw new BizException(502, "文件存储服务异常");
            }
        }
    }

    private void removeObject(String bucket, String objectKey) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    }

    private void ensureBucket(String bucket) throws Exception {
        if (readyBuckets.contains(bucket)) {
            return;
        }
        synchronized (readyBuckets) {
            if (readyBuckets.contains(bucket)) {
                return;
            }
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            readyBuckets.add(bucket);
        }
    }
}
