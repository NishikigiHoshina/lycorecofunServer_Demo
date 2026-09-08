package com.lycorisfun.fun.Service.impl;

import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.NewsMapper;
import com.lycorisfun.fun.Service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

    @Override
    public void delById(Integer id) {

    }

    @Override
    public void add(Post userInfo) {

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
