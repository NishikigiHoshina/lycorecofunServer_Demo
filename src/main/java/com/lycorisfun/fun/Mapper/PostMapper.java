package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {
    public List<Post> findAll();
    public Post findById(int id);

    public int insert(Post post);
    public int delbyid(int id);

    public Post findByTitle(String title);
    public Post findByContent(String content);
    public Post findByTitleAndContent(String title, String content);
    public Post findBytime(String time);
    public Post findByUser(String user);

    public int save(Post post);

}
