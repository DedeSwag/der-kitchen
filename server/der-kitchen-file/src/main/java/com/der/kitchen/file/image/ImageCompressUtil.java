package com.der.kitchen.file.image;

import lombok.Data;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

/**
 * 图片压缩与缩略图生成工具
 */
@Component
public class ImageCompressUtil {

    private static final long COMPRESS_THRESHOLD = 500 * 1024L;
    private static final int THUMBNAIL_WIDTH = 320;
    private static final Set<String> RASTER_EXTS = Set.of("jpg", "jpeg", "png");

    /**
     * 如需压缩则压缩（webp 跳过压缩，直接原样返回）
     */
    public CompressedImage compressIfNeeded(MultipartFile file, String ext) throws Exception {
        byte[] bytes = file.getBytes();
        String contentType = toContentType(ext);

        if (isRaster(ext) && bytes.length > COMPRESS_THRESHOLD) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .scale(1.0)
                    .outputQuality(0.75)
                    .outputFormat("jpg")
                    .toOutputStream(out);
            return new CompressedImage(out.toByteArray(), "jpg", "image/jpeg");
        }
        return new CompressedImage(bytes, ext, contentType);
    }

    /**
     * 生成缩略图（宽度 320px，等比缩放），仅对栅格图有效
     */
    public CompressedImage thumbnail(byte[] source, String ext) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(source))
                .width(THUMBNAIL_WIDTH)
                .outputFormat("jpg")
                .outputQuality(0.7)
                .toOutputStream(out);
        return new CompressedImage(out.toByteArray(), "jpg", "image/jpeg");
    }

    public boolean isRaster(String ext) {
        return RASTER_EXTS.contains(ext.toLowerCase());
    }

    private String toContentType(String ext) {
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    @Data
    public static class CompressedImage {
        private final byte[] bytes;
        private final String extension;
        private final String contentType;
    }
}
