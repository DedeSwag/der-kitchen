package com.der.kitchen.file.service;

import com.der.kitchen.file.vo.FileAccessVO;
import com.der.kitchen.file.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 通用文件服务接口
 */
public interface FileService {

    /**
     * 批量上传文件，返回每个文件的 ID 及预签名访问地址
     */
    List<FileUploadVO> upload(List<MultipartFile> files);

    /**
     * 通过文件 ID 获取预签名访问地址
     */
    FileAccessVO getAccessUrl(Long fileId);

    /**
     * 校验文件存在、属于指定用户且为受支持的图片。
     */
    void validateOwnedImage(Long fileId, Long ownerId);

    /**
     * 删除文件（仅允许上传人删除），同步删除对象存储中的对象
     */
    void delete(Long fileId);
}
