package com.lycorisfun.fun.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

/**
 * 正文文档的文本抽取工具（纯函数，无 Spring 依赖）。
 *
 * <p>三个用途：</p>
 * <ol>
 *   <li>{@link #toExcerpt} —— 从规范 doc 生成 {@code posts.content} 的纯文本摘要
 *       （该列是 varchar(255)，摘要正好落在列宽内，因此**不需要改列类型**）；</li>
 *   <li>{@link #firstImageSrc} —— 取文档中第一张图，回填 {@code posts.imgurl} 作封面；</li>
 *   <li>{@link #legacyExcerpt} —— 存量行的 {@code content} 里是短 HTML，列表页需要一个
 *       纯文本预览，这里做一次保守的标签剥离。**仅用于展示**，输出由前端按纯文本转义渲染。</li>
 * </ol>
 */
public final class PostDocText {

    /** 摘要字符上限：与 posts.content 的 varchar(255) 对齐 */
    public static final int DEFAULT_EXCERPT_CHARS = 255;

    /** 图片在摘要中的占位，避免"纯图片帖子"在列表里显示成空白 */
    private static final String IMAGE_PLACEHOLDER = "[图片]";

    /** 抽取文本时需要与前后文隔开的节点类型（块级 + 列表项 + 图片） */
    private static final Set<String> BLOCK_LIKE =
            Set.of("p", "h2", "h3", "blockquote", "ul", "ol", "pre", "li", "img");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PostDocText() {
    }

    /** 抽取纯文本（图片以 [图片] 占位）；入参为规范 doc JSON，解析失败返回空串 */
    public static String toPlainText(String docJson) {
        if (docJson == null || docJson.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            collect(MAPPER.readTree(docJson), sb);
        } catch (Exception e) {
            return "";
        }
        return collapseWhitespace(sb.toString());
    }

    /** 由 doc 生成不超过 maxChars 的摘要 */
    public static String toExcerpt(String docJson, int maxChars) {
        return truncate(toPlainText(docJson), maxChars);
    }

    /** 取文档中第一张图片的 src；没有图返回 null */
    public static String firstImageSrc(String docJson) {
        if (docJson == null || docJson.isEmpty()) {
            return null;
        }
        try {
            return findFirstImage(MAPPER.readTree(docJson));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 存量行（content 里是 HTML）的列表预览：剥掉 script/style 整块 → 去标签 → 解码常见实体 → 压空白。
     * 输出是纯文本，前端以 {@code {{ }}} 渲染（会被转义），因此这里不承担 XSS 防线。
     */
    public static String legacyExcerpt(String htmlOrText, int maxChars) {
        if (htmlOrText == null || htmlOrText.isEmpty()) {
            return "";
        }
        String s = htmlOrText
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?s)<[^>]*>", " ");
        // 顺序要紧：&amp; 必须最后解码，否则 "&amp;lt;" 会被解成 "<"（双重解码）
        s = s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
        return truncate(collapseWhitespace(s), maxChars);
    }

    /* ==================== 内部实现 ==================== */

    private static void collect(JsonNode node, StringBuilder sb) {
        if (node == null) {
            return;
        }
        if (node.isTextual()) {
            sb.append(node.asText());
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collect(child, sb);
            }
            return;
        }
        if (node.isObject()) {
            JsonNode t = node.get("t");
            String type = (t != null && t.isTextual()) ? t.asText() : null;
            // 块级之间留一个空格，否则摘要会变成"第一段第二段"这种连读
            // （行内标记之间**不加**，它们是同一句话的一部分）
            // 注意 type 可能为 null（整篇文档的根对象就没有 t）：Set.of(...) 是不可变集合，
            // contains(null) 会直接抛 NPE，所以必须先判 null。
            if (type != null && BLOCK_LIKE.contains(type)) {
                appendSeparator(sb);
            }
            if ("img".equals(type)) {
                sb.append(IMAGE_PLACEHOLDER);
                return;                     // 图片节点无文本子节点
            }
            // 两种入参形态都要接受：单个节点的子内容在 c 下，而整篇文档 {v,nodes} 的子内容在 nodes 下。
            collect(node.get("c"), sb);
            collect(node.get("nodes"), sb);
        }
    }

    /** 若已累积内容且末尾不是空格，就补一个空格 */
    private static void appendSeparator(StringBuilder sb) {
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) != ' ') {
            sb.append(' ');
        }
    }

    private static String findFirstImage(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findFirstImage(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (node.isObject()) {
            JsonNode t = node.get("t");
            if (t != null && "img".equals(t.asText())) {
                JsonNode src = node.get("src");
                return src != null && src.isTextual() ? src.asText() : null;
            }
            // 同 collect：既支持单个节点（子内容在 c），也支持整篇文档（子内容在 nodes）
            String inChildren = findFirstImage(node.get("c"));
            return inChildren != null ? inChildren : findFirstImage(node.get("nodes"));
        }
        return null;
    }

    private static String collapseWhitespace(String s) {
        return s.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    /** 按字符截断（库为 utf8mb3，字符必为 BMP，直接 substring 不会切开代理对） */
    private static String truncate(String s, int maxChars) {
        if (maxChars <= 0 || s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars);
    }
}
