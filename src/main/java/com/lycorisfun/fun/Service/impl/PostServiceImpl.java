package com.lycorisfun.fun.Service.impl;


import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.PostMapper;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {
    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Post> findAll() {
        List<Post> postList = postMapper.findAll();
        if (postList == null || postList.size()==0) {
            // 抛业务异常，状态码 404
            throw new BusinessException(404, "查询失败：未找到任何帖子数据");
        }
        return postList;
    }

    @Override
    public List<Post> findlist(Integer findnum){
        if (findnum==null){
            findnum=10;
        }
        List<Post> postList = postMapper.findlist(findnum);
        if (postList == null || postList.size()==0) {
            throw new BusinessException(404,"查询失败：未找到任何帖子数据");
        }
        return postList;
    }

    @Override
    public int delById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "删除失败：帖子ID无效（不能为null或负数或0）");
        }
        int affectedRows = postMapper.delbyid(id);
        if (affectedRows == 0) {
            throw new BusinessException(404, "删除失败：ID为" + id + "的帖子不存在");
        }
        return affectedRows;
    }


    @Override
    public void add(Post post) {
        if (post == null) {
            throw new BusinessException(400, "新增失败：帖子内容不能为null");
        }

        post.setCreated_at(LocalDateTime.now().toString());

        if(post.getTitle().isEmpty()){
            post.setTitle("无标题");
        }
        post.setStatus(1);
        post.setReply_count(0);
        post.setLike_count(0);
        post.setPost_username(userMapper.findById(post.getPost_userid()).getUserName());
        int affectedRows = postMapper.add(post);
        if (affectedRows != 1) {
            throw new BusinessException(500, "新增失败：插入数据未生效（影响行数：" + affectedRows + "）");
        }

    }


    @Override
    public Post findById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "查询失败：帖子ID无效（不能为null或负数或0）");
        }
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(404, "查询失败：ID为" + id + "的帖子不存在");
        }
        return post;
    }

    @Override
    public  List<Post> findByUser(String username){
        if (username==null || username.trim().length()==0){
            throw new BusinessException(400,"查询失败，没有该用户");
        }
        List<Integer>userid=new ArrayList<Integer>();
        List<User> userInfoList = userMapper.findByUsername(username);
        List<Post> postList = new ArrayList<Post>();
        for (User user : userInfoList) {
            userid.add(user.getUserId());
        }
        if (userid.size()==0){
            throw new BusinessException(400,"无结果");
        }
        for (Integer id : userid){
            postList.addAll(postMapper.findByUserid(id));
        }
        return postList;
    }

    @Override
    public List<Post> findByTitle(String title) {
        if(title==null || title.trim().length()==0){
            throw new BusinessException(400,"params is null");
        }
        List<Post> list=postMapper.findByTitle(title);
        if (list==null || list.size()==0){
            throw new BusinessException(404,"not found any post");
        }
        System.out.println("find "+list.size()+" posts");
        return list;
    }


//    @Override
//    public void updatePostInfo(Post userInfo) {
//        if (userInfo == null) {
//            throw new BusinessException(400, "更新失败：帖子内容不能为null");
//        }
//        int affectedRows = postMapper.updatePostInfo(userInfo);
//        if (affectedRows != 1) {
//            throw new BusinessException(500, "更新失败：数据修改未生效（影响行数：" + affectedRows + "）");
//        }
//    }

    @Override
    public Post updatePostInfo(Post postInfo){
        if (postInfo == null) {
            throw new BusinessException(400,"修改失败，未查找到用户");
        }
        Post p=new Post();
        if(postInfo.getPostid()==0){
            throw new BusinessException(400,"修改失败，未查找到用户");
        }
        p.setPostid(postInfo.getPostid());//通过id匹配用户对象
        if(postInfo.getPost_userid()!=0){
            p.setPost_userid(postInfo.getPost_userid());
        }
        if(postInfo.getPost_username()!=null){
            p.setPost_username(postInfo.getPost_username());
        }
        if(postInfo.getContent()!=null){
            p.setContent(postInfo.getContent());
        }
        if(postInfo.getCreated_at()!=null){
            p.setCreated_at(postInfo.getCreated_at());
        }
        if(postInfo.getTitle()!=null){
            p.setTitle(postInfo.getTitle());
        }
        if(postInfo.getLink()!=null){
            p.setLink(postInfo.getLink());
        }
        if(postInfo.getImgurl()!=null){
            p.setImgurl(postInfo.getImgurl());
        }
        if(postInfo.getLike_count()!=0){
            p.setLike_count(postInfo.getLike_count());
        }
        if(postInfo.getReply_count()!=0){
            p.setReply_count(postInfo.getReply_count());
        }
        if(postInfo.getRoot_id()!=0){
            p.setRoot_id(postInfo.getRoot_id());
        }
        if(postInfo.getParent_id()!=0){
            p.setParent_id(postInfo.getParent_id());
        }

        int n=postMapper.updatePostInfo(p);
        System.out.println("修改成功，影响:"+n+"行");
        return p;
    }
}
