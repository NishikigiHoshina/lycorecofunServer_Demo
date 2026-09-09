package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Interceptor.TokenInterceptor;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.NewsService;
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

    // 校验当前登录用户为管理员：DB 判定，不信任前端提交的 status
    private void requireNewsAdmin(HttpServletRequest request) {
        Integer uid = TokenInterceptor.currentUserId(request);
        if (uid == null) {
            throw new BusinessException(401, "未登录");
        }
        User u = userMapper.findById(uid);          // uid 已判空，可安全拆箱
        if (u == null || u.getStatus() != 3) {
            throw new BusinessException(403, "无权操作：仅管理员可管理新闻");
        }
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

}
