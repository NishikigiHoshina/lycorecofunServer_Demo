package com.lycorisfun.fun.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lycorisfun.fun.Exception.BusinessException;

import java.util.Locale;
import java.util.Set;

/**
 * 帖子正文文档（结构化 JSON，v1）的校验与规范化 —— 本模块的**唯一信任边界**。
 *
 * <p>设计取舍：正文不存 HTML，只存白名单节点树。渲染端按 schema 出标签、文本一律走框架转义，
 * 因此不存在"HTML 注入"这一类漏洞，也就不需要 DOMPurify / Jsoup 之类的清洗器 ——
 * 没有清洗器，就没有可被绕过的清洗器。安全性由本类的白名单校验承担。</p>
 *
 * <p>schema（v1）：
 * <pre>
 * Doc    = { "v":1, "nodes":[Block...] }
 * Block  = {"t":"p"|"h2"|"h3"|"blockquote", "c":[Inline...]}
 *        | {"t":"ul"|"ol", "c":[{"t":"li","c":[Inline...]}...]}
 *        | {"t":"pre", "c":["代码文本"]}
 *        | {"t":"img", "src":"/upload/...", "alt":"..."}
 * Inline = {"t":"b"|"i"|"u"|"code", "c":[Inline...]}
 *        | {"t":"a", "href":"http(s)/mailto", "c":[Inline...]}
 *        | {"t":"img", "src":"/upload/...", "alt":"..."}   // 行内图（编辑器的实际产出形态）
 *        | "纯文本"
 * </pre>
 *
 * <p>各类上限见下方常量；任何不合规输入一律抛 {@code BusinessException(400)}，不做静默降级
 * （静默降级会让"显示的内容"与"提交的内容"不一致，反而难排查）。规范化输出的 JSON
 * 完全由本类生成，未识别的键会被**拒绝**而非丢弃。</p>
 *
 * <p>本类为纯函数、无 Spring 依赖，便于单元测试；需要配置的"允许的图片前缀"由调用方传入。</p>
 */
public final class PostDocValidator {

    /** 文档版本号；将来 schema 变更时递增，便于按版本分支处理 */
    public static final int VERSION = 1;

    /** 正文 JSON 总长上限（DB 列是 MEDIUMTEXT/16MB，这里给业务上限，避免超大文档 DoS 与库膨胀） */
    public static final int MAX_JSON_CHARS = 64 * 1024;
    /** 块级 + 行内节点总数上限 */
    public static final int MAX_NODES = 500;
    /** 图片数量上限 */
    public static final int MAX_IMAGES = 20;
    /** 行内嵌套层数上限（b>i>u>code 这类叠加） */
    public static final int MAX_INLINE_DEPTH = 4;
    /** 单个文本节点长度上限 */
    public static final int MAX_TEXT_CHARS = 5000;
    /** 图片 alt 长度上限 */
    public static final int MAX_ALT_CHARS = 200;
    /** 链接 href 长度上限 */
    public static final int MAX_HREF_CHARS = 500;
    /** 图片 src 长度上限（与 posts.imgurl / 上传 URL 的量级一致） */
    public static final int MAX_SRC_CHARS = 600;

    private static final Set<String> BLOCK_TYPES =
            Set.of("p", "h2", "h3", "blockquote", "ul", "ol", "pre", "img");
    /**
     * 行内白名单。**含 `img`**：wangEditor 插入图片产出的是 `<p><img src=…></p>` 这种
     * **行内**形态，而不是独立块。前端转换器与渲染器从一开始就支持行内图，
     * 这里若漏掉 img 就会出现"编辑器能插、渲染能显示，唯独提交被 400 拒掉"的怪象。
     */
    private static final Set<String> INLINE_TYPES =
            Set.of("b", "i", "u", "code", "a", "img");
    /** 允许的链接协议白名单；不在此列一律拒绝（javascript:/data:/vbscript:/file: 等） */
    private static final Set<String> LINK_SCHEMES =
            Set.of("http", "https", "mailto");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PostDocValidator() {
    }

    /**
     * 校验并规范化正文文档。
     *
     * @param rawJson             客户端提交的文档 JSON（字符串）
     * @param allowedSrcPrefixes  允许的图片 src 前缀（来自上传配置的 urlPath，如 "/upload"）；
     *                            用于阻断外链追踪像素与 data: URI
     * @return 规范化后的 JSON 字符串（只含白名单内容，可直接落库）
     */
    public static String validateAndNormalize(String rawJson, Set<String> allowedSrcPrefixes) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            throw new BusinessException(400, "正文不能为空");
        }
        if (rawJson.length() > MAX_JSON_CHARS) {
            throw new BusinessException(400, "正文超出长度上限（" + MAX_JSON_CHARS + " 字符）");
        }
        // 库为 utf8mb3，emoji 等 4 字节字符无法入库：在解析前就拦掉并给友好提示
        TextValidator.requireStorable(rawJson, "正文");

        JsonNode root = read(rawJson);
        if (!root.isObject()) {
            throw new BusinessException(400, "正文格式非法：应为对象");
        }
        requireNoUnknownKeys(root, Set.of("v", "nodes"), "正文");
        JsonNode v = root.get("v");
        if (v == null || !v.isInt() || v.asInt() != VERSION) {
            throw new BusinessException(400, "正文格式非法：版本号应为 " + VERSION);
        }
        JsonNode nodes = root.get("nodes");
        if (nodes == null || !nodes.isArray()) {
            throw new BusinessException(400, "正文格式非法：nodes 应为数组");
        }
        if (nodes.size() == 0) {
            throw new BusinessException(400, "正文不能为空");
        }
        if (nodes.size() > MAX_NODES) {
            throw new BusinessException(400, "正文节点数超出上限（" + MAX_NODES + "）");
        }

        Ctx ctx = new Ctx();
        ArrayNode out = MAPPER.createArrayNode();
        for (JsonNode n : nodes) {
            ObjectNode block = normalizeBlock(n, ctx, allowedSrcPrefixes);
            if (block != null) {
                out.add(block);
            }
        }
        // 全为空段落/空列表 → 等同于没有正文
        if (out.size() == 0) {
            throw new BusinessException(400, "正文不能为空");
        }

        ObjectNode result = MAPPER.createObjectNode();
        result.put("v", VERSION);
        result.set("nodes", out);
        String canonical = write(result);
        if (canonical.length() > MAX_JSON_CHARS) {
            throw new BusinessException(400, "正文超出长度上限（" + MAX_JSON_CHARS + " 字符）");
        }
        return canonical;
    }

    /* ==================== 块级 ==================== */

    private static ObjectNode normalizeBlock(JsonNode n, Ctx ctx, Set<String> allowedSrcPrefixes) {
        if (!n.isObject()) {
            throw bad("正文格式非法：块节点应为对象");
        }
        String type = requireType(n);
        if (!BLOCK_TYPES.contains(type)) {
            throw bad("正文含有不支持的块类型：" + type);
        }
        countNode(ctx);

        switch (type) {
            case "img":
                return normalizeImg(n, ctx, allowedSrcPrefixes);
            case "pre":
                return normalizePre(n);
            case "ul":
            case "ol":
                return normalizeList(n, type, ctx, allowedSrcPrefixes);
            default:                 // p / h2 / h3 / blockquote
                return normalizeTextBlock(n, type, ctx, allowedSrcPrefixes);
        }
    }

    private static ObjectNode normalizeImg(JsonNode n, Ctx ctx, Set<String> allowedSrcPrefixes) {
        requireNoUnknownKeys(n, Set.of("t", "src", "alt"), "img");
        ctx.images++;
        if (ctx.images > MAX_IMAGES) {
            throw bad("正文图片数超出上限（" + MAX_IMAGES + "）");
        }
        String src = requireText(n, "src", "img", MAX_SRC_CHARS);
        requireAllowedSrc(src, allowedSrcPrefixes);
        ObjectNode o = MAPPER.createObjectNode();
        o.put("t", "img");
        o.put("src", src);
        String alt = optionalText(n, "alt", MAX_ALT_CHARS, "img");
        o.put("alt", alt == null ? "" : alt);
        return o;
    }

    private static ObjectNode normalizePre(JsonNode n) {
        requireNoUnknownKeys(n, Set.of("t", "c"), "pre");
        JsonNode c = n.get("c");
        if (c == null || !c.isArray() || c.size() != 1 || !c.get(0).isTextual()) {
            throw bad("pre 节点应包含且仅包含一段文本");
        }
        String code = c.get(0).asText();
        if (code.length() > MAX_TEXT_CHARS) {
            throw bad("代码块超出长度上限（" + MAX_TEXT_CHARS + " 字符）");
        }
        String trimmed = code.strip();
        if (trimmed.isEmpty()) {
            return null;             // 空代码块丢弃
        }
        ObjectNode o = MAPPER.createObjectNode();
        o.put("t", "pre");
        o.putArray("c").add(trimmed);
        return o;
    }

    private static ObjectNode normalizeList(JsonNode n, String type, Ctx ctx,
                                            Set<String> allowedSrcPrefixes) {
        requireNoUnknownKeys(n, Set.of("t", "c"), type);
        JsonNode c = n.get("c");
        if (c == null || !c.isArray()) {
            throw bad(type + " 节点应包含列表项数组");
        }
        ArrayNode items = MAPPER.createArrayNode();
        for (JsonNode li : c) {
            if (!li.isObject()) {
                throw bad("列表项应为对象");
            }
            String liType = requireType(li);
            if (!"li".equals(liType)) {
                throw bad("列表项的 t 应为 li，实际为：" + liType);
            }
            requireNoUnknownKeys(li, Set.of("t", "c"), "li");
            countNode(ctx);
            ArrayNode inline = normalizeInlineArray(li.get("c"), ctx, 1, allowedSrcPrefixes);
            if (inline.size() > 0) {
                ObjectNode item = MAPPER.createObjectNode();
                item.put("t", "li");
                item.set("c", inline);
                items.add(item);
            }
        }
        if (items.size() == 0) {
            return null;             // 空列表丢弃
        }
        ObjectNode o = MAPPER.createObjectNode();
        o.put("t", type);
        o.set("c", items);
        return o;
    }

    private static ObjectNode normalizeTextBlock(JsonNode n, String type, Ctx ctx,
                                                 Set<String> allowedSrcPrefixes) {
        requireNoUnknownKeys(n, Set.of("t", "c"), type);
        ArrayNode inline = normalizeInlineArray(n.get("c"), ctx, 1, allowedSrcPrefixes);
        if (inline.size() == 0) {
            // 丢弃空段落：wangEditor 的空行会产出 <p><br></p>，留着重渲染就是一堆空行
            return null;
        }
        ObjectNode o = MAPPER.createObjectNode();
        o.put("t", type);
        o.set("c", inline);
        return o;
    }

    /* ==================== 行内 ==================== */

    private static ArrayNode normalizeInlineArray(JsonNode arr, Ctx ctx, int depth,
                                                  Set<String> allowedSrcPrefixes) {
        if (arr == null || !arr.isArray()) {
            throw bad("行内内容应为数组");
        }
        ArrayNode out = MAPPER.createArrayNode();
        for (JsonNode n : arr) {
            JsonNode normalized = normalizeInline(n, ctx, depth, allowedSrcPrefixes);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return out;
    }

    private static JsonNode normalizeInline(JsonNode n, Ctx ctx, int depth,
                                            Set<String> allowedSrcPrefixes) {
        // 纯文本节点
        if (n.isTextual()) {
            String s = n.asText();
            if (s.length() > MAX_TEXT_CHARS) {
                throw bad("单段文本超出长度上限（" + MAX_TEXT_CHARS + " 字符）");
            }
            return s.isEmpty() ? null : MAPPER.getNodeFactory().textNode(s);
        }
        if (!n.isObject()) {
            throw bad("行内节点应为文本或对象");
        }
        if (depth > MAX_INLINE_DEPTH) {
            throw bad("行内嵌套层数超出上限（" + MAX_INLINE_DEPTH + "）");
        }
        String type = requireType(n);
        if (!INLINE_TYPES.contains(type)) {
            throw bad("正文含有不支持的行内类型：" + type);
        }
        countNode(ctx);

        if ("img".equals(type)) {
            // 行内图与块级图共用同一套校验（src 必须本站上传、数量上限、alt 长度）
            return normalizeImg(n, ctx, allowedSrcPrefixes);
        }
        if ("a".equals(type)) {
            requireNoUnknownKeys(n, Set.of("t", "c", "href"), "a");
            String href = requireText(n, "href", "a", MAX_HREF_CHARS);
            requireSafeHref(href);
            ArrayNode c = normalizeInlineArray(n.get("c"), ctx, depth + 1, allowedSrcPrefixes);
            if (c.size() == 0) {
                return null;
            }
            ObjectNode o = MAPPER.createObjectNode();
            o.put("t", "a");
            o.put("href", href);
            o.set("c", c);
            return o;
        }
        requireNoUnknownKeys(n, Set.of("t", "c"), type);
        ArrayNode c = normalizeInlineArray(n.get("c"), ctx, depth + 1, allowedSrcPrefixes);
        if (c.size() == 0) {
            return null;
        }
        ObjectNode o = MAPPER.createObjectNode();
        o.put("t", type);
        o.set("c", c);
        return o;
    }

    /* ==================== 通用校验 ==================== */

    /** 必须存在且为文本的 t 字段 */
    private static String requireType(JsonNode n) {
        JsonNode t = n.get("t");
        if (t == null || !t.isTextual() || t.asText().isEmpty()) {
            throw bad("节点缺少合法的 t 字段");
        }
        return t.asText();
    }

    /**
     * 拒绝未知键。用白名单而非"丢弃未知键"：静默丢弃会让攻击者的构造"看起来成功"，
     * 也让前端 bug 无声通过，两者都不利于排查。
     */
    private static void requireNoUnknownKeys(JsonNode n, Set<String> allowed, String where) {
        var it = n.fieldNames();
        while (it.hasNext()) {
            String k = it.next();
            if (!allowed.contains(k)) {
                throw bad(where + " 节点含有不允许的字段：" + k);
            }
        }
    }

    private static String requireText(JsonNode n, String field, String where, int max) {
        JsonNode v = n.get(field);
        if (v == null || !v.isTextual() || v.asText().trim().isEmpty()) {
            throw bad(where + " 节点缺少 " + field);
        }
        String s = v.asText().trim();
        if (s.length() > max) {
            throw bad(where + " 的 " + field + " 超出长度上限（" + max + " 字符）");
        }
        return s;
    }

    private static String optionalText(JsonNode n, String field, int max, String where) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        if (!v.isTextual()) {
            throw bad(where + " 的 " + field + " 应为文本");
        }
        String s = v.asText().trim();
        if (s.length() > max) {
            throw bad(where + " 的 " + field + " 超出长度上限（" + max + " 字符）");
        }
        return s;
    }

    /**
     * 链接协议白名单。对形如 "java\nscript:" 这类夹控制字符的混淆写法，取到的 scheme
     * 不会等于白名单值，因而同样被拒 —— 白名单比黑名单在这里更稳。
     */
    private static void requireSafeHref(String href) {
        int colon = href.indexOf(':');
        if (colon <= 0) {
            throw bad("链接缺少协议：仅支持 http/https/mailto");
        }
        String scheme = href.substring(0, colon).toLowerCase(Locale.ROOT);
        if (!LINK_SCHEMES.contains(scheme)) {
            throw bad("链接协议不被允许：" + scheme);
        }
        if (("http".equals(scheme) || "https".equals(scheme))
                && !href.regionMatches(true, colon + 1, "//", 0, 2)) {
            throw bad("链接格式非法");
        }
    }

    /** 图片 src 必须是本站上传前缀，阻断外链追踪像素与 data: URI */
    private static void requireAllowedSrc(String src, Set<String> allowedPrefixes) {
        if (allowedPrefixes == null || allowedPrefixes.isEmpty()) {
            throw bad("服务端未配置上传路径，无法接收正文图片");
        }
        String lower = src.toLowerCase(Locale.ROOT);
        for (String prefix : allowedPrefixes) {
            if (prefix != null && !prefix.isEmpty() && lower.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return;
            }
        }
        throw bad("正文图片必须来自本站上传（src 需以 " + String.join(" 或 ", allowedPrefixes) + " 开头）");
    }

    private static void countNode(Ctx ctx) {
        ctx.nodes++;
        if (ctx.nodes > MAX_NODES) {
            throw bad("正文节点数超出上限（" + MAX_NODES + "）");
        }
    }

    /* ==================== JSON 读写 ==================== */

    private static JsonNode read(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(400, "正文格式非法：不是合法 JSON");
        }
    }

    private static String write(JsonNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new BusinessException(500, "正文序列化失败");
        }
    }

    private static BusinessException bad(String msg) {
        return new BusinessException(400, msg);
    }

    /** 计数上下文（节点数 / 图片数），随一次校验生命周期 */
    private static final class Ctx {
        int nodes;
        int images;
    }
}
