package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {
    public List<Post> findAll();
    public List<Post> findlist(Integer findnum);
    public Post findById(int id);

    public int add(Post post);
    public int delbyid(int id);

    public List<Post> findByTitle(String title);
    public Post findByContent(String content);
    //public Post findByTitleAndContent(String title, String content);
    public Post findBytime(String time);
    public Post findByUser(int userid);
    public List<Post> findByUserid(int userid);

    public List<Post> findByParentid(int parentid);
    public List<Post> findByRootid(int rootid);

    public int updatePostInfo(Post post);

}
