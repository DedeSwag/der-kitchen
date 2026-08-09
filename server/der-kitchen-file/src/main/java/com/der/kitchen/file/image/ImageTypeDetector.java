package com.der.kitchen.file.image;

import com.der.kitchen.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * 通过文件签名识别允许的图片类型，不信任文件名和客户端 Content-Type。
 */
@Component
public class ImageTypeDetector {

    private static final Set<String> JPEG_EXTENSIONS = Set.of("jpg", "jpeg");

    public ImageType detect(byte[] bytes, String filenameExtension) {
        ImageType detected = detectSignature(bytes);
        String extension = filenameExtension == null ? "" : filenameExtension.toLowerCase(Locale.ROOT);
        boolean matches = detected.extension().equals(extension)
                || (detected.extension().equals("jpg") && JPEG_EXTENSIONS.contains(extension));
        if (!matches) {
            throw new BizException("文件扩展名与实际图片类型不一致");
        }
        validateImage(bytes, detected);
        return detected;
    }

    private ImageType detectSignature(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new BizException("无法识别图片类型");
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return new ImageType("jpg", "image/jpeg");
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return new ImageType("png", "image/png");
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new ImageType("webp", "image/webp");
        }
        throw new BizException("仅支持真实的 jpg/jpeg/png/webp 图片");
    }

    private void validateImage(byte[] bytes, ImageType type) {
        int width;
        int height;
        if (type.extension().equals("webp")) {
            int[] dimensions = readWebpDimensions(bytes);
            width = dimensions[0];
            height = dimensions[1];
        } else {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null) {
                    throw new BizException("图片内容已损坏或无法解析");
                }
                width = image.getWidth();
                height = image.getHeight();
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException("图片内容已损坏或无法解析");
            }
        }
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0 || width > 10_000 || height > 10_000 || pixels > 40_000_000L) {
            throw new BizException("图片尺寸不合法或像素过大");
        }
    }

    private int[] readWebpDimensions(byte[] bytes) {
        if (bytes.length < 30) {
            throw new BizException("WebP图片内容已损坏");
        }
        String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if ("VP8X".equals(chunk)) {
            return new int[]{1 + littleEndian24(bytes, 24), 1 + littleEndian24(bytes, 27)};
        }
        if ("VP8 ".equals(chunk) && bytes.length >= 30
                && (bytes[23] & 0xFF) == 0x9D && bytes[24] == 0x01 && bytes[25] == 0x2A) {
            int width = littleEndian16(bytes, 26) & 0x3FFF;
            int height = littleEndian16(bytes, 28) & 0x3FFF;
            return new int[]{width, height};
        }
        if ("VP8L".equals(chunk) && bytes.length >= 25 && (bytes[20] & 0xFF) == 0x2F) {
            int bits = (bytes[21] & 0xFF)
                    | ((bytes[22] & 0xFF) << 8)
                    | ((bytes[23] & 0xFF) << 16)
                    | ((bytes[24] & 0xFF) << 24);
            return new int[]{(bits & 0x3FFF) + 1, ((bits >>> 14) & 0x3FFF) + 1};
        }
        throw new BizException("WebP图片内容已损坏或格式不受支持");
    }

    private int littleEndian16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private int littleEndian24(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16);
    }

    public record ImageType(String extension, String contentType) {
    }
}
