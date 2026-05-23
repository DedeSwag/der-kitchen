package com.der.kitchen.file.controller;

import com.der.kitchen.common.annotation.RequireRole;
import com.der.kitchen.common.result.R;
import com.der.kitchen.file.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/v1/admin/files")
@RequireRole("admin")
@RequiredArgsConstructor
public class FileController {

    private final MinioService minioService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = minioService.upload(file);
        return R.ok(Map.of("url", url));
    }
}
