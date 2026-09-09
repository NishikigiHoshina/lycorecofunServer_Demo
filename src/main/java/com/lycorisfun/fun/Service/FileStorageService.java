package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.config.UploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

/**
 * 统一文件上传落盘：类型白名单 + 扩展名白名单 + 魔数校验 + 大小限制 + uuid 命名。
 * 所有新上传入口都走这里，安全策略只维护一份。
 * 落盘 = root/{type.dir}/{uuid}.{ext}；对外 URL = {type.urlPath}/{uuid}.{ext}。
 */
@Service
public class FileStorageService {

    @Autowired
    private UploadProperties uploadProps;

    /** 按扩展名校验的图片魔数前缀（十六进制 / ASCII） */
    private boolean magicMatches(MultipartFile file, String ext) throws IOException {
        byte[] head = new byte[12];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.read(head);
        }
        if (n < 0) {
            return false;
        }
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg":
            case "jpeg":
                return n >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png":
                return n >= 8
                        && (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47
                        && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
            case "gif":
                return n >= 6
                        && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8'
                        && (head[4] == '7' || head[4] == '9') && head[5] == 'a';
            case "webp":
                return n >= 12
                        && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            default:
                return false;
        }
    }

    private String extOf(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        // 过滤隐藏文件/无意义后缀
        if (!ext.matches("[a-z0-9]{1,8}")) {
            return "";
        }
        return ext;
    }

    /** 保存一个文件；返回对外可访问的 URL（相对 host，形如 /upload/xxx.png） */
    public String store(MultipartFile file, String typeKey) {
        UploadProperties.Type type = uploadProps.type(typeKey);
        if (type == null) {
            throw new BusinessException(400, "未知的上传类型：" + typeKey);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件为空");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!type.allows(ext)) {
            throw new BusinessException(400, "不支持的文件类型，仅允许：" + String.join("/", type.getExtensions()));
        }
        if (file.getSize() > type.getMaxSize().toBytes()) {
            throw new BusinessException(400, "文件超出大小限制");
        }
        try {
            if (!magicMatches(file, ext)) {
                throw new BusinessException(400, "文件内容与扩展名不符，可能为伪造文件");
            }
        } catch (IOException e) {
            throw new BusinessException(500, "读取上传文件失败");
        }

        Path base = Paths.get(uploadProps.getRoot()).toAbsolutePath().normalize();
        Path dir = base.resolve(type.getDir()).normalize();
        if (!dir.startsWith(base)) {
            throw new BusinessException(400, "上传目录配置非法");
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name).toFile());
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }
        return type.getUrlPath() + "/" + name;
    }
}
