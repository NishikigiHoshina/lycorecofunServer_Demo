package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.NewsService;
import com.lycorisfun.fun.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）

public class MessageController {
    @Autowired
    NewsService newsService;


    @Autowired
    private UserMapper userMapper;

    @PostMapping("/newslist")
    public List<News> newslist() {
        List<News> newslist = new ArrayList<>();
        newslist=newsService.findLatestNews(6);
        return newslist;
    }

    @PostMapping("/newslistAll")
    public List<News> newslistAll() {
        List<News> newslist = new ArrayList<>();
        newslist=newsService.findAllNews();
        return newslist;
    }

    @PostMapping("/getAnnouncement")
    public String getAnnouncement() {
//        String Announcement="网站仍在施工，请稍后再来支持！";
        //未来加数据库查询
        String Announcement=newsService.findAnnouncement().getNews_link();
        return Announcement;
    }

    /* ===== 后台新闻管理（仅管理员 status==3） ===== */

    // 校验当前登录用户为管理员：DB 判定，统一走 AuthUtil
    private void requireNewsAdmin(HttpServletRequest request) {
        AuthUtil.requireAdmin(request, userMapper);
    }

    // 新建新闻（后台"新闻管理"页弹窗提交入口）
    @PostMapping("/addnews")
    @RequireToken
    public Map<String, Object> addNews(@RequestBody News news, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        requireNewsAdmin(request);
        newsService.add(news);
        map.put("code", 200);
        map.put("msg", "发布成功");
        map.put("news_id", news.getNews_id());   // 自增回填
        return map;
    }

    // 删除新闻（软删除 status=0）
    @PostMapping("/deletenews")
    @RequireToken
    public Map<String, Object> deleteNews(@RequestParam Integer news_id, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        requireNewsAdmin(request);
        newsService.delById(news_id);
        map.put("code", 200);
        map.put("msg", "删除成功");
        return map;
    }

    // 编辑新闻（后台"新闻管理"编辑对话框）
    @PostMapping("/updatenews")
    @RequireToken
    public Map<String, Object> updateNews(@RequestBody News news, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        requireNewsAdmin(request);
        newsService.update(news);
        map.put("code", 200);
        map.put("msg", "保存成功");
        return map;
    }

    // 更新全站公告（后台"站内管理"页）。公告即 news 表中 imgUrl 为 NULL 的那行
    @PostMapping("/updateAnnouncement")
    @RequireToken
    public Map<String, Object> updateAnnouncement(@RequestBody Map<String, String> body,
                                                 HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        requireNewsAdmin(request);
        newsService.updateAnnouncement(body == null ? null : body.get("content"));
        map.put("code", 200);
        map.put("msg", "公告已更新");
        return map;
    }

}
