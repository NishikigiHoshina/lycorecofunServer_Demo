package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.Post;

import java.util.List;

public interface PostService {
    public List<Post> findAll();
    public List<Post> findlist(Integer findnum);

    public int delById(Integer id);

    public void add(Post userInfo);

    public Post findById(Integer id);

    public List<Post> findByUser(String username);

    public List<Post> findByTitle(String title);

//    public List<Post> findByUserid(Integer userid);

    public Post updatePostInfo(Post postInfo);

}

