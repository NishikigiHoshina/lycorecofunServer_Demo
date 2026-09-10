package com.lycorisfun.fun.unit;

import com.lycorisfun.fun.util.PostDocText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 正文文本抽取单元测试（纯函数，不依赖 Spring 与数据库）。
 *
 * <p>覆盖三件事：摘要（写入 {@code posts.content}）、正文首图（回填 {@code posts.imgurl} 作封面）、
 * 以及存量帖 HTML 的列表预览（{@code legacyExcerpt}）。</p>
 *
 * <p>注意：这两个方法接收的是**整篇文档** {@code {v,nodes}}，而不是单个节点 ——
 * 早期版本正是错在这里（遍历没走进根对象的 {@code nodes}），导致摘要恒为空、封面恒为 null，
 * 因此这里特意同时覆盖"整篇文档"与"单节点"两种入参形态。</p>
 */
class PostDocTextTest {

    private static String doc(String nodesJson) {
        return "{\"v\":1,\"nodes\":[" + nodesJson + "]}";
    }

    @Nested
    @DisplayName("摘要抽取 toExcerpt")
    class Excerpt {

        @Test
        @DisplayName("整篇文档：文本按顺序拼接")
        void fromWholeDoc() {
            String d = doc("{\"t\":\"p\",\"c\":[\"第一段\"]},{\"t\":\"p\",\"c\":[\"第二段\"]}");
            assertThat(PostDocText.toExcerpt(d, 255)).isEqualTo("第一段 第二段");
        }

        @Test
        @DisplayName("标记内部的文字也会被抽到（b / a，漏了就会静默丢字）")
        void insideMarks() {
            String d = doc("{\"t\":\"p\",\"c\":[\"见\",{\"t\":\"b\",\"c\":[\"夏亚\"]},\"与\","
                    + "{\"t\":\"a\",\"href\":\"http://x\",\"c\":[\"阿姆罗\"]},\"并肩\"]}");
            assertThat(PostDocText.toExcerpt(d, 255)).isEqualTo("见夏亚与阿姆罗并肩");
        }

        @Test
        @DisplayName("纯图片文档给出 [图片] 占位（否则列表里是一片空白）")
        void imageOnly() {
            String d = doc("{\"t\":\"p\",\"c\":[{\"t\":\"img\",\"src\":\"/upload/post/a.png\",\"alt\":\"\"}]}");
            assertThat(PostDocText.toExcerpt(d, 255)).isEqualTo("[图片]");
        }

        @Test
        @DisplayName("按上限截断")
        void truncates() {
            assertThat(PostDocText.toExcerpt(doc("{\"t\":\"p\",\"c\":[\"一二三四五六七八九十\"]}"), 5))
                    .isEqualTo("一二三四五");
        }

        @Test
        @DisplayName("默认摘要不超过 255 字（适配 posts.content 的列宽）")
        void fitsColumnWidth() {
            String longText = "字".repeat(1000);
            String out = PostDocText.toExcerpt(doc("{\"t\":\"p\",\"c\":[\"" + longText + "\"]}"),
                    PostDocText.DEFAULT_EXCERPT_CHARS);
            assertThat(out).hasSize(255);
            assertThat(out).isEqualTo("字".repeat(255));
        }

        @Test
        @DisplayName("列表项 / 标题 / 引用内的文字也计入")
        void fromOtherBlocks() {
            String d = doc("{\"t\":\"h2\",\"c\":[\"标题\"]},{\"t\":\"ul\",\"c\":[{\"t\":\"li\",\"c\":[\"项\"]}]},"
                    + "{\"t\":\"blockquote\",\"c\":[\"引用\"]}");
            assertThat(PostDocText.toExcerpt(d, 255)).isEqualTo("标题 项 引用");
        }

        @Test
        @DisplayName("空文档 → 空串")
        void emptyDoc() {
            assertThat(PostDocText.toExcerpt(doc(""), 255)).isEmpty();
            assertThat(PostDocText.toExcerpt(null, 255)).isEmpty();
            assertThat(PostDocText.toExcerpt("", 255)).isEmpty();
        }

        @Test
        @DisplayName("非法 JSON → 空串（不抛异常，保证发帖不因此失败）")
        void malformedJson() {
            assertThat(PostDocText.toExcerpt("{坏掉的", 255)).isEmpty();
        }
    }

    @Nested
    @DisplayName("首图抽取 firstImageSrc")
    class FirstImage {

        @Test
        @DisplayName("整篇文档：取第一张行内图")
        void firstInline() {
            String d = doc("{\"t\":\"p\",\"c\":[\"文字\"]},"
                    + "{\"t\":\"p\",\"c\":[{\"t\":\"img\",\"src\":\"/upload/post/first.png\",\"alt\":\"\"}]},"
                    + "{\"t\":\"p\",\"c\":[{\"t\":\"img\",\"src\":\"/upload/post/second.png\",\"alt\":\"\"}]}");
            assertThat(PostDocText.firstImageSrc(d)).isEqualTo("/upload/post/first.png");
        }

        @Test
        @DisplayName("块级图片同样可取")
        void blockImage() {
            assertThat(PostDocText.firstImageSrc(doc("{\"t\":\"img\",\"src\":\"/upload/post/b.png\",\"alt\":\"\"}")))
                    .isEqualTo("/upload/post/b.png");
        }

        @Test
        @DisplayName("无图文档 → null")
        void noImage() {
            assertThat(PostDocText.firstImageSrc(doc("{\"t\":\"p\",\"c\":[\"只有文字\"]}"))).isNull();
            assertThat(PostDocText.firstImageSrc(doc(""))).isNull();
            assertThat(PostDocText.firstImageSrc(null)).isNull();
        }

        @Test
        @DisplayName("单节点入参也支持（兼容两种调用形态）")
        void acceptsSingleNode() {
            assertThat(PostDocText.firstImageSrc("{\"t\":\"p\",\"c\":[{\"t\":\"img\","
                    + "\"src\":\"/upload/post/x.png\",\"alt\":\"\"}]}"))
                    .isEqualTo("/upload/post/x.png");
        }
    }

    @Nested
    @DisplayName("存量帖 HTML 的列表预览 legacyExcerpt")
    class Legacy {

        @Test
        @DisplayName("剥掉标签、解码实体")
        void stripsTags() {
            assertThat(PostDocText.legacyExcerpt("<p>&gt; 测试</p>", 255)).isEqualTo("> 测试");
        }

        @Test
        @DisplayName("实体只解一层（&amp;lt; 应得 &lt; 而不是 <）")
        void noDoubleDecode() {
            assertThat(PostDocText.legacyExcerpt("<p>&amp;lt;</p>", 255)).isEqualTo("&lt;");
        }

        @Test
        @DisplayName("script / style 连内容一起丢弃")
        void dropsScriptAndStyle() {
            assertThat(PostDocText.legacyExcerpt("<script>alert(1)</script>", 255)).isEmpty();
            assertThat(PostDocText.legacyExcerpt("<style>a{}</style>正文", 255)).isEqualTo("正文");
        }

        @Test
        @DisplayName("内联样式被剥掉但文字保留（老帖的 span 颜色/字号会丢，属预期取舍）")
        void keepsTextLosesStyle() {
            // 注意：标签被替换成空格，所以行内 span 处会留下一个空格（"嗷 嗷嗷"）。
            // 这是"标签→空格"这个简单策略的副产物：好处是块级标签能保住词间距，
            // 代价是行内 span 会多一个空格。对列表预览无实质影响，故不特殊处理。
            assertThat(PostDocText.legacyExcerpt(
                    "<p>嗷<span style=\"color: rgb(247, 89, 171);\">嗷嗷</span></p>", 255))
                    .isEqualTo("嗷 嗷嗷");
        }

        @Test
        @DisplayName("超长按上限截断")
        void truncates() {
            assertThat(PostDocText.legacyExcerpt("<p>" + "字".repeat(500) + "</p>", 255)).hasSize(255);
        }
    }
}
