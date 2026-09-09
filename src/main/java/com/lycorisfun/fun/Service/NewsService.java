package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.News;

import java.util.List;


public interface NewsService {
    public List<News> findAll();

    public void delById(Integer id);

    public void add(News news);

    public void update(News news);

    public News findById(Integer id);

    public List<News> findByTitle(String title);

    public List<News> findAllNews();

    public List<News> findLatestNews(Integer num);

    public News findAnnouncement();
}
