package com.lycorisfun.fun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传配置（不硬编码在代码/Controller）。
 * 来源：application.properties 的 `lycorisfun.upload.*`，见后端 README。
 * 设计：一个"资源类型"对应一组 {key, urlPath, dir, extensions, maxSize}，
 * 未来新增上传入口只需在配置里追加一个类型，存储/校验/静态映射自动跟随。
 */
@Component
@ConfigurationProperties(prefix = "lycorisfun.upload")
public class UploadProperties {

    /** 上传根目录（跨平台默认：用户主目录；可被配置/环境变量覆盖） */
    private String root = System.getProperty("user.home") + "/lycorisfun-upload";
    private List<Type> types = new ArrayList<>();

    public String getRoot() {
        return root;
    }
    public void setRoot(String root) {
        this.root = root;
    }
    public List<Type> getTypes() {
        return types;
    }
    public void setTypes(List<Type> types) {
        this.types = types;
    }

    /** 全部有效类型；配置为空时注入 index-img 安全默认，避免空配 NPE */
    public List<Type> activeTypes() {
        if (types == null) {
            types = new ArrayList<>();
        }
        if (types.isEmpty()) {
            types.add(defaultIndexImg());
        }
        return types;
    }

    /** 按 key 取类型；不存在返回 null */
    public Type type(String key) {
        if (key == null) {
            return null;
        }
        for (Type t : activeTypes()) {
            if (key.equalsIgnoreCase(t.getKey())) {
                return t;
            }
        }
        return null;
    }

    private Type defaultIndexImg() {
        Type t = new Type();
        t.setKey("index-img");
        t.setUrlPath("/upload");
        t.setDir("img");
        t.setExtensions(new ArrayList<>(List.of("jpg", "jpeg", "png", "gif", "webp")));
        t.setMaxSize(DataSize.ofMegabytes(5));
        return t;
    }

    public static class Type {
        private String key;
        /** 对外访问 URL 前缀，如 /upload */
        private String urlPath;
        /** 相对 root 的落盘子目录，如 img */
        private String dir;
        private List<String> extensions = new ArrayList<>();
        /** 支持可读单位：5MB / 512KB 等（Spring DataSize） */
        private DataSize maxSize = DataSize.ofMegabytes(5);

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getUrlPath() { return urlPath; }
        public void setUrlPath(String urlPath) { this.urlPath = urlPath; }
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public List<String> getExtensions() { return extensions; }
        public void setExtensions(List<String> extensions) { this.extensions = extensions; }
        public DataSize getMaxSize() { return maxSize; }
        public void setMaxSize(DataSize maxSize) { this.maxSize = maxSize; }

        public boolean allows(String ext) {
            if (ext == null || extensions == null) {
                return false;
            }
            for (String e : extensions) {
                if (ext.equalsIgnoreCase(e)) {
                    return true;
                }
            }
            return false;
        }
    }
}
