package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.config.UploadProperties;
import com.lycorisfun.fun.util.PostDocText;
import com.lycorisfun.fun.util.PostDocValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 正文文档的应用层入口：把"需要配置"的部分（允许的图片 src 前缀）与
 * {@link PostDocValidator} 的纯校验逻辑缝合起来，让 Controller/其他 Service
 * 不必关心上传配置。
 *
 * <p>不设接口：这里只是一组无状态函数的薄封装，多一层接口没有实际收益。</p>
 */
@Service
public class PostDocService {

    @Autowired
    private UploadProperties uploadProps;

    /** 校验并规范化正文；不合规抛 BusinessException(400) */
    public String validateAndNormalize(String rawJson) {
        return PostDocValidator.validateAndNormalize(rawJson, allowedSrcPrefixes());
    }

    /** 由规范正文生成 posts.content 的纯文本摘要（≤255 字，适配现有列宽） */
    public String toExcerpt(String canonicalDoc) {
        return PostDocText.toExcerpt(canonicalDoc, PostDocText.DEFAULT_EXCERPT_CHARS);
    }

    /** 取正文首图，用于回填 posts.imgurl 作封面；无图返回 null */
    public String firstImageSrc(String canonicalDoc) {
        return PostDocText.firstImageSrc(canonicalDoc);
    }

    /**
     * 允许出现在正文里的图片 src 前缀 = 上传配置里各资源类型的 urlPath。
     * 配置为空时 {@code activeTypes()} 会给出 index-img 的安全默认值，故这里不会是空集。
     */
    public Set<String> allowedSrcPrefixes() {
        Set<String> prefixes = new LinkedHashSet<>();
        for (UploadProperties.Type t : uploadProps.activeTypes()) {
            if (t.getUrlPath() != null && !t.getUrlPath().isEmpty()) {
                prefixes.add(t.getUrlPath());
            }
        }
        return prefixes;
    }
}
