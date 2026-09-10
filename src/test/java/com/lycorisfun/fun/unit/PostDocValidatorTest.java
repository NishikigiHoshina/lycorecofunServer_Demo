package com.lycorisfun.fun.unit;

import com.lycorisfun.fun.util.PostDocValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 正文文档校验器单元测试（纯函数，不依赖 Spring 与数据库）。
 *
 * <p>这个类是整个帖子链路的**唯一信任边界**，因此用例分为两组：
 * 「应当放行」与「应当拒绝」—— 后者覆盖伪造节点、协议绕过、越权图片、超限等攻击面。</p>
 */
class PostDocValidatorTest {

    /** 与 UploadProperties 中的上传 urlPath 对应 */
    private static final Set<String> PREFIXES = Set.of("/upload", "/upload/post");

    private static String doc(String nodesJson) {
        return "{\"v\":1,\"nodes\":[" + nodesJson + "]}";
    }

    /** 整篇文档：正文是一个"只含一张行内图"的段落（编辑器插图的真实形态） */
    private static String inlineImg(String src) {
        return doc("{\"t\":\"p\",\"c\":[{\"t\":\"img\",\"src\":\"" + src + "\",\"alt\":\"\"}]}");
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("应当放行：合法文档")
    class Accepted {

        @Test
        @DisplayName("块级图片")
        void blockImage() {
            assertThatCode(() -> PostDocValidator.validateAndNormalize(
                    doc("{\"t\":\"img\",\"src\":\"/upload/post/a.png\",\"alt\":\"\"}"), PREFIXES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("行内图片（wangEditor 插图的真实形态：图片在段落内部）")
        void inlineImage() {
            assertThatCode(() -> PostDocValidator.validateAndNormalize(inlineImg("/upload/post/a.png"), PREFIXES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("图文混排 + 嵌套标记")
        void mixedInline() {
            String json = doc("{\"t\":\"p\",\"c\":[\"文字\",{\"t\":\"b\",\"c\":[\"加粗\"]},"
                    + "{\"t\":\"img\",\"src\":\"/upload/post/a.png\",\"alt\":\"说明\"},\"尾巴\"]}");
            assertThatCode(() -> PostDocValidator.validateAndNormalize(json, PREFIXES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("标题 / 列表 / 引用 / 代码块")
        void otherBlockTypes() {
            String json = doc("{\"t\":\"h2\",\"c\":[\"标题\"]},"
                    + "{\"t\":\"h3\",\"c\":[\"小标题\"]},"
                    + "{\"t\":\"ul\",\"c\":[{\"t\":\"li\",\"c\":[\"项一\"]},{\"t\":\"li\",\"c\":[\"项二\"]}]},"
                    + "{\"t\":\"ol\",\"c\":[{\"t\":\"li\",\"c\":[\"一\"]}]},"
                    + "{\"t\":\"blockquote\",\"c\":[\"引用\"]},"
                    + "{\"t\":\"pre\",\"c\":[\"select 1;\"]}");
            assertThatCode(() -> PostDocValidator.validateAndNormalize(json, PREFIXES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("合法链接（https / mailto）")
        void safeLinks() {
            String json = doc("{\"t\":\"p\",\"c\":[{\"t\":\"a\",\"href\":\"https://example.com\",\"c\":[\"站外\"]}]},"
                    + "{\"t\":\"p\",\"c\":[{\"t\":\"a\",\"href\":\"mailto:a@b.com\",\"c\":[\"邮件\"]}]}");
            assertThatCode(() -> PostDocValidator.validateAndNormalize(json, PREFIXES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("图片数刚好到上限 20 张")
        void exactlyMaxImages() {
            assertThatCode(() -> PostDocValidator.validateAndNormalize(imagesDoc(20), PREFIXES))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("规范化会丢弃空段落（<p><br></p> 这类噪声）")
        void dropsEmptyBlocks() {
            String json = doc("{\"t\":\"p\",\"c\":[\"有内容\"]},{\"t\":\"p\",\"c\":[]},{\"t\":\"p\",\"c\":[\"  \"]}");
            String out = PostDocValidator.validateAndNormalize(json, PREFIXES);
            assertThat(out).contains("有内容");
            assertThat(out.split("\"t\":\"p\"", -1).length - 1).isEqualTo(1);
        }

        @Test
        @DisplayName("输出为服务端产出的规范 JSON（版本号被写回、未知字段不会被带出）")
        void outputIsCanonical() {
            String out = PostDocValidator.validateAndNormalize(inlineImg("/upload/post/a.png"), PREFIXES);
            assertThat(out).startsWith("{\"v\":1,\"nodes\":[");
            assertThat(out).contains("\"src\":\"/upload/post/a.png\"");
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("应当拒绝：攻击面与错误输入")
    class Rejected {

        @Test
        @DisplayName("行内图片的外链 src（阻断追踪像素）")
        void externalInlineImage() {
            assertRejected(inlineImg("https://evil.test/x.png"), "本站上传");
        }

        @Test
        @DisplayName("块级图片的外链 src")
        void externalBlockImage() {
            assertRejected(doc("{\"t\":\"img\",\"src\":\"https://evil.test/x.png\",\"alt\":\"\"}"), "本站上传");
        }

        @Test
        @DisplayName("data: 协议图片")
        void dataUriImage() {
            assertRejected(inlineImg("data:image/png;base64,AAAA"), "本站上传");
        }

        @Test
        @DisplayName("javascript: 链接")
        void javascriptHref() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[{\"t\":\"a\",\"href\":\"javascript:alert(1)\",\"c\":[\"x\"]}]}"),
                    "协议不被允许");
        }

        @Test
        @DisplayName("夹了控制字符的协议混淆（java\\nscript:）")
        void obfuscatedScheme() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[{\"t\":\"a\",\"href\":\"java\\nscript:alert(1)\",\"c\":[\"x\"]}]}"),
                    null);
        }

        @Test
        @DisplayName("未知块类型 script（HTML 注入面）")
        void scriptBlock() {
            assertRejected(doc("{\"t\":\"script\",\"c\":[\"alert(1)\"]}"), "不支持的块类型");
        }

        @Test
        @DisplayName("未知行内类型 iframe")
        void iframeInline() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[{\"t\":\"iframe\",\"c\":[\"x\"]}]}"), "不支持的行内类型");
        }

        @Test
        @DisplayName("节点上的未知字段（onclick 等事件属性）")
        void unknownField() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[\"a\"],\"onclick\":\"alert(1)\"}"), "不允许的字段");
        }

        @Test
        @DisplayName("版本号不匹配")
        void wrongVersion() {
            assertRejected("{\"v\":2,\"nodes\":[{\"t\":\"p\",\"c\":[\"a\"]}]}", "版本号");
        }

        @Test
        @DisplayName("非法 JSON")
        void malformedJson() {
            assertRejected("{不是 JSON", "不是合法 JSON");
        }

        @Test
        @DisplayName("emoji（库为 utf8mb3，只支持 BMP）")
        void emoji() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[\"笑死 😀\"]}"), "特殊字符");
        }

        @Test
        @DisplayName("空正文（nodes 为空数组）")
        void emptyNodes() {
            assertRejected(doc(""), "不能为空");
        }

        @Test
        @DisplayName("只有空段落的正文")
        void onlyEmptyBlocks() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[]}"), "不能为空");
        }

        @Test
        @DisplayName("纯空白段落不算正文（否则能建出摘要为空的帖子）")
        void blankOnlyParagraph() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[\"   \"]}"), "不能为空");
            assertRejected(doc("{\"t\":\"p\",\"c\":[\"\\n\"]}"), "不能为空");
        }

        @Test
        @DisplayName("图片超过上限 21 张")
        void tooManyImages() {
            assertRejected(imagesDoc(21), "图片数超出上限");
        }

        @Test
        @DisplayName("单个文本节点超长")
        void textTooLong() {
            assertRejected(doc("{\"t\":\"p\",\"c\":[\"" + "字".repeat(5001) + "\"]}"), "长度上限");
        }

        @Test
        @DisplayName("节点总数超限")
        void tooManyNodes() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 501; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append("{\"t\":\"p\",\"c\":[\"行").append(i).append("\"]}");
            }
            assertRejected("{\"v\":1,\"nodes\":[" + sb + "]}", "节点数超出上限");
        }

        @Test
        @DisplayName("行内嵌套层数超限")
        void tooDeepInline() {
            String json = doc("{\"t\":\"p\",\"c\":[{\"t\":\"b\",\"c\":[{\"t\":\"i\",\"c\":[{\"t\":\"u\",\"c\":["
                    + "{\"t\":\"code\",\"c\":[{\"t\":\"b\",\"c\":[\"太深了\"]}]}]}]}]}]}");
            assertRejected(json, "嵌套层数超出上限");
        }

        @Test
        @DisplayName("null / 空串入参")
        void nullInput() {
            assertRejected(null, "不能为空");
            assertRejected("   ", "不能为空");
        }
    }

    /* ==================================================================== */

    private static String imagesDoc(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"t\":\"p\",\"c\":[{\"t\":\"img\",\"src\":\"/upload/post/")
                    .append(i).append(".png\",\"alt\":\"\"}]}");
        }
        return doc(sb.toString());
    }

    /** 断言被拒；expectedFragment 为 null 时只要求"被拒" */
    private static void assertRejected(String json, String expectedFragment) {
        assertThatThrownBy(() -> PostDocValidator.validateAndNormalize(json, PREFIXES))
                .isInstanceOf(RuntimeException.class)
                .satisfies(e -> {
                    if (expectedFragment != null) {
                        assertThat(e.getMessage()).contains(expectedFragment);
                    }
                });
    }
}
