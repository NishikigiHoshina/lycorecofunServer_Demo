package com.lycorisfun.fun.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 帖子配图上传接口测试。
 *
 * <p>该接口是**唯一对普通登录用户开放的上传入口**，因此安全校验是本类的重点：
 * 扩展名白名单、图片魔数校验、大小上限、解压炸弹防护（像素总量）、未登录拦截。</p>
 *
 * <p>落盘目录已由基类改到 {@code target/test-upload}，用例结束后整体清理 ——
 * 事务回滚管不到磁盘文件，不改就会往真实上传目录里扔垃圾。</p>
 */
@DisplayName("配图上传接口")
class UploadApiTest extends ApiTestBase {

    private static final Path TEST_UPLOAD_ROOT = Path.of("target", "test-upload");

    @AfterAll
    static void cleanUploadDir() throws IOException {
        if (!Files.exists(TEST_UPLOAD_ROOT)) {
            return;
        }
        try (var walk = Files.walk(TEST_UPLOAD_ROOT)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 清理失败不影响测试结论
                }
            });
        }
    }

    /* ==================== 构造上传文件 ==================== */

    /** 一张真实可解码的 PNG */
    private static byte[] realPng(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /**
     * 手工构造一个"解压炸弹"：文件只有几十字节，但 IHDR 里声称 20000×20000（4 亿像素）。
     * 这里刻意把 IHDR 的 CRC 算对，好让 ImageIO 能读出宽高 —— 正是为了验证
     * "先只读文件头校验像素、再真正解码"这道防护真的生效（若先解码，内存就已经爆了）。
     */
    private static byte[] decompressionBombPng() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

        ByteArrayOutputStream dataBuf = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(dataBuf);
        data.writeInt(20000);          // width
        data.writeInt(20000);          // height
        data.writeByte(8);             // bit depth
        data.writeByte(2);             // color type: truecolor
        data.writeByte(0);             // compression
        data.writeByte(0);             // filter
        data.writeByte(0);             // interlace
        byte[] ihdr = dataBuf.toByteArray();

        byte[] type = {'I', 'H', 'D', 'R'};
        out.write(new byte[]{0, 0, 0, 13});       // chunk length = 13
        out.write(type);
        out.write(ihdr);
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(ihdr);
        DataOutputStream tail = new DataOutputStream(out);
        tail.writeInt((int) crc.getValue());
        return out.toByteArray();
    }

    private MvcResult upload(Cookie cookie, String fieldName, String filename, String contentType, byte[] bytes)
            throws Exception {
        var req = multipart("/api/uploadPostImg")
                .file(new MockMultipartFile(fieldName, filename, contentType, bytes));
        if (cookie != null) {
            req.cookie(cookie);
        }
        return mvc.perform(req).andReturn();
    }

    /* ==================================================================== */

    @Test
    @DisplayName("未登录上传 → 401")
    void uploadNeedsLogin() throws Exception {
        assertThat(upload(null, "file", "a.png", "image/png", realPng(4, 4)).getResponse().getStatus())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("普通登录用户上传合法 PNG → 200，返回相对路径且文件确实落盘")
    void uploadOk() throws Exception {
        Cookie cookie = login(newNormalUser("upload_user"));
        MvcResult res = upload(cookie, "file", "shot.png", "image/png", realPng(8, 8));

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        var b = body(res);
        assertThat(b.get("code").asInt()).isEqualTo(200);

        String url = b.get("url").asText();
        assertThat(url).startsWith("/upload/post/").endsWith(".png");   // 相对路径，便于换域名

        // 相对 URL → 落盘文件：root + dir(=post) + 文件名
        Path stored = TEST_UPLOAD_ROOT.resolve("post").resolve(url.substring("/upload/post/".length()));
        assertThat(Files.exists(stored)).as("文件应落到 %s", stored).isTrue();
        assertThat(Files.size(stored)).isPositive();

        // 重编码后的图片应当仍可被 ImageIO 解码（证明不是坏文件）
        assertThat(ImageIO.read(stored.toFile())).isNotNull();
    }

    @Test
    @DisplayName("内容是文本、只把后缀改成 .png → 400（魔数校验）")
    void fakePngRejected() throws Exception {
        Cookie cookie = login(newNormalUser("upload_fake"));
        MvcResult res = upload(cookie, "file", "fake.png", "image/png",
                "这不是图片，只是文本".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("内容与扩展名不符");
    }

    @Test
    @DisplayName("扩展名不在白名单（.txt）→ 400")
    void badExtensionRejected() throws Exception {
        Cookie cookie = login(newNormalUser("upload_ext"));
        MvcResult res = upload(cookie, "file", "note.txt", "text/plain", "hello".getBytes());
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("不支持的文件类型");
    }

    @Test
    @DisplayName("超过 5MB 上限 → 400")
    void oversizeRejected() throws Exception {
        Cookie cookie = login(newNormalUser("upload_big"));
        byte[] big = new byte[5 * 1024 * 1024 + 1024];
        // 至少要像一张 PNG 才会走到"大小"这一关，故补上魔数头
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 0, big, 0, 8);
        MvcResult res = upload(cookie, "file", "big.png", "image/png", big);
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("超出大小限制");
    }

    @Test
    @DisplayName("解压炸弹（极小文件声称 4 亿像素）→ 400，且提示像素上限而非 OOM")
    void decompressionBombRejected() throws Exception {
        Cookie cookie = login(newNormalUser("upload_bomb"));
        byte[] bomb = decompressionBombPng();
        assertThat(bomb.length).as("炸弹文件本身应很小").isLessThan(100);

        MvcResult res = upload(cookie, "file", "bomb.png", "image/png", bomb);
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("像素");
    }

    @Test
    @DisplayName("空文件 → 400")
    void emptyFileRejected() throws Exception {
        Cookie cookie = login(newNormalUser("upload_empty"));
        MvcResult res = upload(cookie, "file", "empty.png", "image/png", new byte[0]);
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
    }
}
