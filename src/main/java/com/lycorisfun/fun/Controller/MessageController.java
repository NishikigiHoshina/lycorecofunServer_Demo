package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Entity.News;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效

public class MessageController {
    @PostMapping("/newslist")
    public List<News> newslist() {
        List<News> newslist = new ArrayList<>();
        newslist.add(new News(0,
                "喫茶伴手礼BOX【完全限定版】Blu-ray 计划发售！",
                "https://free.picui.cn/free/2025/10/19/68f4f5b42eddf.png",
                "https://x.com/lycoris_recoil/status/1978400991432364408"));
        newslist.add(new News(1,
                "大阪烧 × 莉可丽丝 联动决定",
                "https://free.picui.cn/free/2025/10/19/68f4f5b3cb705.jpg",
                "https://www.yukarichan.co.jp/collabo2025/lycoris_recoil.html"));
        newslist.add(new News(2,
                "KENTEX × 莉可丽丝 联动手表预定开始",
                "https://free.picui.cn/free/2025/10/19/68f4f5b342bcc.jpg",
                "https://kentex-shop.com/lycoris2025/"));
        newslist.add(new News(3,
                "新作小说《夜之街》第二话更新了！" ,
                "https://free.picui.cn/free/2025/10/19/68f4f5b318870.jpg",
                "https://note.com/lyco_reco/n/nb0ab7ca9bade"));
        return newslist;
    }

    @PostMapping("/getAnnouncement")
    public String getAnnouncement() {
        String Announcement="网站仍在施工，请稍后再来支持！";
        //未来加数据库查询
        return Announcement;
    }

}
