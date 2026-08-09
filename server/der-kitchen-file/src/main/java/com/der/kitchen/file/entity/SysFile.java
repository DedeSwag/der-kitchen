package com.der.kitchen.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用文件元数据记录，对应表 sys_file
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    /** 原始文件名 */
    private String originalName;

    /** MinIO 对象键（路径） */
    private String fileKey;

    /** 所在桶名 */
    private String bucket;

    /** 文件大小（字节） */
    private Long sizeBytes;

    /** MIME 类型 */
    private String mimeType;

    /** 缩略图对象键，非图片为空 */
    private String thumbKey;

    /** 缩略图大小（字节） */
    private Long thumbSizeBytes;

    /** 缩略图 MIME 类型 */
    private String thumbMimeType;

    /** 上传人 userId */
    private Long uploadBy;
}
