package com.der.kitchen.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.file.config.MinioProperties;
import com.der.kitchen.file.entity.SysFile;
import com.der.kitchen.file.image.ImageCompressUtil;
import com.der.kitchen.file.image.ImageTypeDetector;
import com.der.kitchen.file.mapper.SysFileMapper;
import com.der.kitchen.file.minio.MinioTemplate;
import com.der.kitchen.file.service.FileService;
import com.der.kitchen.file.service.FileReferenceChecker;
import com.der.kitchen.file.vo.FileAccessVO;
import com.der.kitchen.file.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final SysFileMapper sysFileMapper;
    private final MinioTemplate minioTemplate;
    private final MinioProperties minioProperties;
    private final ImageCompressUtil imageCompressUtil;
    private final ImageTypeDetector imageTypeDetector;
    private final List<FileReferenceChecker> referenceCheckers;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileUploadVO> upload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        if (files.size() > 5) {
            throw new BizException("单次最多上传5个文件");
        }
        String bucket = minioProperties.getBucket();
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        List<FileUploadVO> result = new ArrayList<>();
        List<StoredObject> uploadedObjects = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                if (file.getSize() > MAX_SIZE) {
                    throw new BizException("文件大小不能超过 5MB");
                }
                String original = file.getOriginalFilename();
                if (original != null && original.length() > 255) {
                    throw new BizException("文件名不能超过255个字符");
                }
                String ext = FileUtil.extName(StrUtil.blankToDefault(original, ""))
                        .toLowerCase(Locale.ROOT);
                if (StrUtil.isBlank(ext) || !ALLOWED_EXT.contains(ext)) {
                    throw new BizException("仅支持 jpg/jpeg/png/webp 格式");
                }

                ImageCompressUtil.CompressedImage prepared;
                try {
                    ImageTypeDetector.ImageType type = imageTypeDetector.detect(file.getBytes(), ext);
                    prepared = imageCompressUtil.compressIfNeeded(file, type.extension());
                } catch (BizException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("图片处理失败: type={}", e.getClass().getSimpleName());
                    throw new BizException("图片处理失败");
                }

                String objectId = UUID.randomUUID().toString().replace("-", "");
                String objectKey = "images/" + day + "/" + objectId + "." + prepared.getExtension();
                minioTemplate.upload(bucket, objectKey,
                        new ByteArrayInputStream(prepared.getBytes()),
                        prepared.getBytes().length,
                        prepared.getContentType());
                uploadedObjects.add(new StoredObject(bucket, objectKey));

                String thumbKey = null;
                ImageCompressUtil.CompressedImage thumbnail = null;
                if (imageCompressUtil.isRaster(prepared.getExtension())) {
                    try {
                        thumbnail = imageCompressUtil.thumbnail(prepared.getBytes(), prepared.getExtension());
                        thumbKey = "images/" + day + "/thumb/" + objectId + "_thumb."
                                + thumbnail.getExtension();
                        minioTemplate.upload(bucket, thumbKey,
                                new ByteArrayInputStream(thumbnail.getBytes()),
                                thumbnail.getBytes().length,
                                thumbnail.getContentType());
                        uploadedObjects.add(new StoredObject(bucket, thumbKey));
                    } catch (Exception e) {
                        log.warn("缩略图生成失败，已跳过: type={}", e.getClass().getSimpleName());
                        thumbKey = null;
                        thumbnail = null;
                    }
                }

                SysFile row = new SysFile();
                row.setOriginalName(original != null ? original : objectKey);
                row.setFileKey(objectKey);
                row.setBucket(bucket);
                row.setSizeBytes((long) prepared.getBytes().length);
                row.setMimeType(prepared.getContentType());
                row.setThumbKey(thumbKey);
                row.setThumbSizeBytes(thumbnail != null ? (long) thumbnail.getBytes().length : null);
                row.setThumbMimeType(thumbnail != null ? thumbnail.getContentType() : null);
                row.setUploadBy(SecurityUtils.requireUserId());
                sysFileMapper.insert(row);

                String url = minioTemplate.getPresignedUrl(bucket, objectKey);
                String thumbnailUrl = StrUtil.isNotBlank(thumbKey)
                        ? minioTemplate.getPresignedUrl(bucket, thumbKey)
                        : null;

                result.add(FileUploadVO.builder()
                        .fileId(row.getId())
                        .originalName(row.getOriginalName())
                        .url(url)
                        .thumbnailUrl(thumbnailUrl)
                        .build());
            }

            if (result.isEmpty()) {
                throw new BizException("没有有效的上传文件");
            }
            return result;
        } catch (RuntimeException e) {
            cleanupUploadedObjects(uploadedObjects);
            throw e;
        }
    }

    @Override
    public FileAccessVO getAccessUrl(Long fileId) {
        SysFile row = sysFileMapper.selectById(fileId);
        if (row == null) {
            throw new BizException(404, "文件不存在");
        }
        String url = minioTemplate.getPresignedUrl(row.getBucket(), row.getFileKey());
        String thumbnailUrl = StrUtil.isNotBlank(row.getThumbKey())
                ? minioTemplate.getPresignedUrl(row.getBucket(), row.getThumbKey())
                : null;
        return FileAccessVO.builder()
                .fileId(fileId)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    @Override
    public void validateOwnedImage(Long fileId, Long ownerId) {
        SysFile row = sysFileMapper.selectById(fileId);
        if (row == null) {
            throw new BizException(404, "图片文件不存在");
        }
        if (!row.getUploadBy().equals(ownerId)) {
            throw new BizException(403, "只能使用本人上传的图片");
        }
        if (row.getMimeType() == null || !row.getMimeType().startsWith("image/")) {
            throw new BizException("所选文件不是图片");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long fileId) {
        Long userId = SecurityUtils.requireUserId();
        SysFile row = sysFileMapper.selectById(fileId);
        if (row == null) {
            throw new BizException(404, "文件不存在");
        }
        if (row.getUploadBy() != null && !row.getUploadBy().equals(userId)) {
            throw new BizException(403, "只能删除本人上传的文件");
        }
        for (FileReferenceChecker checker : referenceCheckers) {
            if (checker.isReferenced(fileId)) {
                throw new BizException(409, "文件正在被" + checker.referenceName() + "使用，无法删除");
            }
        }
        minioTemplate.delete(row.getBucket(), row.getFileKey());
        if (StrUtil.isNotBlank(row.getThumbKey())) {
            minioTemplate.delete(row.getBucket(), row.getThumbKey());
        }
        sysFileMapper.deleteById(fileId);
    }

    private void cleanupUploadedObjects(List<StoredObject> uploadedObjects) {
        for (int i = uploadedObjects.size() - 1; i >= 0; i--) {
            StoredObject object = uploadedObjects.get(i);
            try {
                minioTemplate.delete(object.bucket(), object.key());
            } catch (RuntimeException cleanupError) {
                log.error("上传回滚清理对象失败: bucket={}, key={}, type={}", object.bucket(), object.key(),
                        cleanupError.getClass().getSimpleName());
            }
        }
    }

    private record StoredObject(String bucket, String key) {
    }
}
