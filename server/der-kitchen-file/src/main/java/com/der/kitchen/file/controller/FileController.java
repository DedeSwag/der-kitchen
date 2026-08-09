package com.der.kitchen.file.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.file.service.FileService;
import com.der.kitchen.file.vo.FileAccessVO;
import com.der.kitchen.file.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 管理端文件接口（管理员可上传/访问，并仅可删除本人上传的文件）
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/common/file")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件（支持批量，最多5个）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<List<FileUploadVO>> upload(
            @Parameter(description = "文件列表") @RequestParam("files") MultipartFile[] files) {
        SecurityUtils.requireUserId();
        return R.ok(fileService.upload(Arrays.asList(files)));
    }

    @Operation(summary = "获取文件预签名访问地址")
    @GetMapping("/{fileId}")
    public R<FileAccessVO> accessUrl(
            @Parameter(description = "文件ID") @PathVariable @Positive Long fileId) {
        return R.ok(fileService.getAccessUrl(fileId));
    }

    @Operation(summary = "删除文件（仅上传人可删除）")
    @DeleteMapping("/{fileId}")
    public R<Void> delete(
            @Parameter(description = "文件ID") @PathVariable @Positive Long fileId) {
        fileService.delete(fileId);
        return R.ok();
    }
}
