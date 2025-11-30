package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Entity.Post;

import java.util.List;


public interface NewsService {
    public List<News> findAll();

    public void delById(Integer id);

    public void add(Post userInfo);

    public News findById(Integer id);

    public List<News> findByTitle(String title);

    public List<News> findAllNews();

    public List<News> findLatestNews(Integer num);

    public News findAnnouncement();
}
