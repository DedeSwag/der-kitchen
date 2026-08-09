package com.der.kitchen.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件访问地址 VO（通过 fileId 查询时返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAccessVO {

    /** 文件 ID */
    private Long fileId;

    /** 预签名访问地址（短期有效） */
    private String url;

    /** 缩略图预签名访问地址，非图片文件为空 */
    private String thumbnailUrl;
}
