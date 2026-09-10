package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.config.UploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * 统一文件上传落盘：类型白名单 + 扩展名白名单 + 魔数校验 + 大小限制 + uuid 命名。
 * 所有新上传入口都走这里，安全策略只维护一份。
 * 落盘 = root/{type.dir}/{uuid}.{ext}；对外 URL = {type.urlPath}/{uuid}.{ext}。
 *
 * <p>两条落盘路径：</p>
 * <ul>
 *   <li>{@link #store} —— 原样落盘。用于管理员上传的站内宣传图等既有入口（不动既有行为）。</li>
 *   <li>{@link #storeImage} —— **解码后重新编码**再落盘。用于普通登录用户可上传的帖子配图：
 *       新文件完全由解码出的像素生成，EXIF/ICC 元数据与"追加在文件尾部的任意载荷"（polyglot 文件）
 *       都会被丢掉。这是允许非管理员上传文件时性价比最高的一道加固。</li>
 * </ul>
 *
 * <p>{@code storeImage} 对 jpg/png 重编码；gif/webp 因 JDK 自带编码器不支持（webp）或会丢失动图
 * （gif 只解出首帧），退化为原样落盘 —— 此时仍受魔数校验保护。</p>
 */
@Service
public class FileStorageService {

    @Autowired
    private UploadProperties uploadProps;

    /** 重编码前的像素总量上限，防"解压炸弹"（极小的文件解码成超大位图导致 OOM） */
    private static final long MAX_PIXELS = 40_000_000L;

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

    /** 保存一个文件（原样落盘）；返回对外可访问的 URL（相对 host，形如 /upload/xxx.png） */
    public String store(MultipartFile file, String typeKey) {
        Target t = prepare(file, typeKey);
        try {
            Files.createDirectories(t.path.getParent());
            file.transferTo(t.path.toFile());
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }
        return t.urlPath() + "/" + t.path.getFileName();
    }

    /**
     * 保存图片：解码 → 重新编码 → 落盘（详见类注释）。
     * jpg/png 走重编码；gif/webp 或 JDK 无对应编码器时退化为原样落盘。
     */
    public String storeImage(MultipartFile file, String typeKey) {
        Target t = prepare(file, typeKey);
        String format = encodableFormat(t.ext);
        try {
            Files.createDirectories(t.path.getParent());
            if (format == null) {
                file.transferTo(t.path.toFile());          // 无编码器 → 原样落盘
                return t.urlPath() + "/" + t.path.getFileName();
            }
            BufferedImage decoded;
            try (InputStream in = file.getInputStream()) {
                decoded = decodeGuarded(in, t.ext);
            }
            if (decoded == null) {
                throw new BusinessException(400, "文件内容无法解码为图片");
            }
            if ("jpg".equals(format)) {
                decoded = toOpaqueRgb(decoded);            // jpg 不支持 alpha，先铺白底
            }
            if (!ImageIO.write(decoded, format, t.path.toFile())) {
                throw new BusinessException(500, "图片重编码失败");
            }
        } catch (IOException e) {
            throw new BusinessException(500, "图片保存失败：" + e.getMessage());
        }
        return t.urlPath() + "/" + t.path.getFileName();
    }

    /* ==================== 内部实现 ==================== */

    /** 校验通过后要落盘的目标 */
    private static final class Target {
        final String ext;
        final Path path;
        final String urlPath;

        Target(String ext, Path path, String urlPath) {
            this.ext = ext;
            this.path = path;
            this.urlPath = urlPath;
        }

        String urlPath() {
            return urlPath;
        }
    }

    /** 全部前置校验：类型/非空/扩展名白名单/大小/魔数/路径穿越防护 */
    private Target prepare(MultipartFile file, String typeKey) {
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
        String name = UUID.randomUUID() + "." + ext;   // 文件名完全由服务端生成，不含用户输入
        return new Target(ext, dir.resolve(name), type.getUrlPath());
    }

    /** 可重编码的格式 → ImageIO 的格式名；返回 null 表示不支持重编码 */
    private static String encodableFormat(String ext) {
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg":
            case "jpeg":
                return "jpg";
            case "png":
                return "png";
            default:
                // webp：JDK 无编码器；gif：会丢动图只留首帧 → 两者都原样落盘
                return null;
        }
    }

    /**
     * 先只读文件头拿到宽高、校验像素总量，再真正解码 —— 顺序很关键：
     * 若先 {@code ImageIO.read} 再判断尺寸，解压炸弹在判断之前就已经把内存吃完了。
     */
    private BufferedImage decodeGuarded(InputStream in, String ext) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                long w = reader.getWidth(0);
                long h = reader.getHeight(0);
                if (w <= 0 || h <= 0) {
                    return null;
                }
                if (w * h > MAX_PIXELS) {
                    throw new BusinessException(400,
                            "图片像素尺寸过大（上限约 " + (MAX_PIXELS / 1_000_000) + " 百万像素）");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /** 铺白底转为无 alpha 的 RGB，供 jpg 编码使用 */
    private static BufferedImage toOpaqueRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, Color.WHITE, null);
        g.dispose();
        return dst;
    }
}
