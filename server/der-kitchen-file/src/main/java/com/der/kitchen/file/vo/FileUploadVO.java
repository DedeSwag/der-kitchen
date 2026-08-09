package com.der.kitchen.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传成功后的响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVO {

    /** 文件 ID，业务表通过此 ID 关联 */
    private Long fileId;

    /** 原始文件名 */
    private String originalName;

    /** 预签名访问地址（短期有效） */
    private String url;

    /** 缩略图预签名访问地址，非图片文件为空 */
    private String thumbnailUrl;
}
