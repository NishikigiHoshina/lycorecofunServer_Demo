package com.lycorisfun.fun.Service.impl;

import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.NewsMapper;
import com.lycorisfun.fun.Service.NewsService;
import com.lycorisfun.fun.util.TextValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NewsServiceImpl implements NewsService {
    @Autowired
    private NewsMapper newsMapper;

    @Override
    public List<News> findAll() {
        List<News> newsList = newsMapper.findAll();
        if (newsList == null || newsList.size()==0) {
            // 抛业务异常，状态码 404
            throw new BusinessException(404, "查询失败：未找到任何帖子数据");
        }
        return newsList;
    }

    // 软删除：status 0（前端 newslistAll 仍返回便于管理；首页 newslist 只展示 status=1）
    @Caching(evict = {
            @CacheEvict(cacheNames = "newsAll", allEntries = true),
            @CacheEvict(cacheNames = "newsLatest", allEntries = true)
    })
    @Override
    public void delById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "删除失败：新闻ID无效");
        }
        int affectedRows = newsMapper.delbyid(id);
        if (affectedRows == 0) {
            throw new BusinessException(404, "删除失败：ID为" + id + "的新闻不存在或已删除");
        }
        System.out.println("删除成功，影响：" + affectedRows + "行");
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "newsAll", allEntries = true),
            @CacheEvict(cacheNames = "newsLatest", allEntries = true)
    })
    @Override
    public void add(News news) {
        if (news == null) {
            throw new BusinessException(400, "新增失败：新闻内容不能为null");
        }
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "新增失败：标题不能为空");
        }
        // 入库前字符校验：库为 utf8mb3，只能存 BMP（拦截 emoji 等 4 字节字符）
        TextValidator.requireStorable(news.getTitle(), "标题");
        TextValidator.requireStorable(news.getImgurl(), "图片链接");
        TextValidator.requireStorable(news.getNews_link(), "新闻链接");
        // 缺省兜底：时间=当前；状态=1（0 已软删除）
        if (news.getTime() == null || news.getTime().trim().isEmpty()) {
            news.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (news.getStatus() == 0) {
            news.setStatus(1);
        }
        int affectedRows = newsMapper.add(news);
        if (affectedRows != 1) {
            throw new BusinessException(500, "新增失败：插入数据未生效（影响行数：" + affectedRows + "）");
        }
        System.out.println("新增成功，影响：" + affectedRows + "行");
    }

    // 编辑新闻（后台"新闻管理"编辑对话框）
    @Caching(evict = {
            @CacheEvict(cacheNames = "newsAll", allEntries = true),
            @CacheEvict(cacheNames = "newsLatest", allEntries = true)
    })
    @Override
    public void update(News news) {
        if (news == null) {
            throw new BusinessException(400, "修改失败：参数为空");
        }
        if (news.getNews_id() <= 0) {
            throw new BusinessException(400, "修改失败：新闻ID无效");
        }
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "修改失败：标题不能为空");
        }
        TextValidator.requireStorable(news.getTitle(), "标题");
        TextValidator.requireStorable(news.getImgurl(), "图片链接");
        TextValidator.requireStorable(news.getNews_link(), "新闻链接");
        int affectedRows = newsMapper.updateNewsInfo(news);
        if (affectedRows != 1) {
            throw new BusinessException(404, "修改失败：ID为" + news.getNews_id() + "的新闻不存在或已删除");
        }
        System.out.println("修改成功，影响：" + affectedRows + "行");
    }

    @Override
    public News findById(Integer id) {
        return null;
    }

    @Override
    public List<News> findByTitle(String title) {
        return List.of();
    }

    @Cacheable(cacheNames = "newsAll")
    @Override
    public List<News> findAllNews(){
        List<News> newsList = newsMapper.findAllNews();
        if (newsList == null || newsList.size()==0) {
            throw new BusinessException(404,"not found any news,maybe server was unavailable");
        }
        return newsList;
    }

    @Cacheable(cacheNames = "newsLatest", key = "#num")
    @Override
    public List<News> findLatestNews(Integer num){
        Integer findnum=(num!=null)?num:6;
        List<News> newsList = newsMapper.findLatestNews(findnum);
        if (newsList == null || newsList.size()==0) {
            throw new BusinessException(404,"not found any news,maybe server was unavailable");
        }
        return newsList;
    }

    @Cacheable(cacheNames = "announcement")
    @Override
    public  News findAnnouncement(){
        News Announcement=new News();
        Announcement=newsMapper.findAnnouncement();
        if (Announcement!=null){
            System.out.println("找到网站公告:"+Announcement.getTitle());
            return Announcement;
        }
        else {
            throw new BusinessException(404,"not found Announcement");
        }
    }
}
