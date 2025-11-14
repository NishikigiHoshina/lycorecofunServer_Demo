package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.Post;

import java.util.List;

public interface PostService {
    public List<Post> findAll();

    public void delById(Integer id);

    public void add(Post userInfo);

    public Post findById(Integer id);

    public void update(Post userInfo);
}

