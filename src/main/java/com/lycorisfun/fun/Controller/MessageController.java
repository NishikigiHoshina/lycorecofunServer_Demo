package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.FuncMapper;
import com.lycorisfun.fun.Service.FuncService;
import com.lycorisfun.fun.Service.NewsService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效

public class MessageController {
    @Autowired
    NewsService newsService;

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



}
