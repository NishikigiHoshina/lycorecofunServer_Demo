package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Entity.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsMapper {
    public List<News> findAll();
    public News findById(int id);

    public int add(News news);
    public int delbyid(int id);
    public List<News> findByTitle(String title);
    public List<News> findAllNews();
    public List<News> findLatestNews(Integer num);
    public News findAnnouncement();
}
