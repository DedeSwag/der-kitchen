package com.der.kitchen.file.image;

import com.der.kitchen.common.exception.BizException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageTypeDetectorTest {

    private final ImageTypeDetector detector = new ImageTypeDetector();

    @Test
    void detectsImageFromContentInsteadOfClientMetadata() throws Exception {
        byte[] png = imageBytes("png");

        assertThat(detector.detect(png, "png"))
                .isEqualTo(new ImageTypeDetector.ImageType("png", "image/png"));
        assertThatThrownBy(() -> detector.detect(png, "jpg"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("扩展名");
    }

    @Test
    void rejectsTruncatedContentEvenWhenSignatureMatches() {
        byte[] fakePng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        };

        assertThatThrownBy(() -> detector.detect(fakePng, "png"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("损坏");
    }

    private byte[] imageBytes(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
