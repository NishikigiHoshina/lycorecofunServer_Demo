package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.Post;

import java.util.List;

public interface PostService {
    public List<Post> findAll();
    public List<Post> findlist(Integer findnum);

    public int delById(Integer id);

    public void add(Post postInfo);

    public void addmessage(Post postInfo);

    public void addcontent(Post postInfo);

    public Post findById(Integer id);

    public List<Post> findByUser(String username);

    public List<Post> findByTitle(String title);

    public List<Post> findContentPointaPost(Integer id);

    public List<Post> findReplyPointaPost(Integer id);

//    public List<Post> findByUserid(Integer userid);

    public Post updatePostInfo(Post postInfo);

    /* ===== 分页查询 ===== */
    public List<Post> findPageList(int page, int size);
    public int countPostList();

}

