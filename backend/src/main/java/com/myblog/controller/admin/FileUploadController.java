package com.myblog.controller.admin;

import com.myblog.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 
 * 将上传的图片保存到服务器本地目录，通过 Nginx 托管静态资源。
 * 开发环境保存到项目根目录 uploads/，生产环境保存到 /www/my-blog/uploads/。
 * 
 * 技术要点：
 * - 文件大小和类型校验
 * - UUID 重命名防止文件名冲突
 * - 按日期分目录存储
 * - 返回可直接访问的 URL
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@PreAuthorize("hasRole('ADMIN')")
public class FileUploadController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.access-url:/uploads}")
    private String accessUrl;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    /**
     * 上传图片
     * POST /api/admin/upload/image
     * 
     * @param file 图片文件（支持 jpg/png/gif/webp，最大 5MB）
     * @return 包含图片访问 URL 的结果
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(400, "文件大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (!isAllowedType(contentType)) {
            return Result.error(400, "只允许上传 jpg/png/gif/webp 格式的图片");
        }

        try {
            // 按日期分目录：uploads/2026/02/25/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dirPath = Paths.get(uploadDir, datePath);
            Files.createDirectories(dirPath);

            // UUID 重命名
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = dirPath.resolve(newFileName);
            file.transferTo(filePath.toFile());

            // 返回可访问的 URL
            String url = accessUrl + "/" + datePath + "/" + newFileName;
            log.info("文件上传成功: {} -> {}", originalName, url);

            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    private boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) return true;
        }
        return false;
    }
}
