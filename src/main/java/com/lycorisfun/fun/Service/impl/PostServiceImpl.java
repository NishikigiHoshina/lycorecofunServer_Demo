package com.lycorisfun.fun.Service.impl;


import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.PostMapper;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void delById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "删除失败：帖子ID无效（不能为null或负数或0）");
        }
        int affectedRows = postMapper.delbyid(id);
        if (affectedRows == 0) {
            throw new BusinessException(404, "删除失败：ID为" + id + "的帖子不存在");
        }

    }


    @Override
    public void add(Post post) {
        if (post == null) {
            throw new BusinessException(400, "新增失败：帖子内容不能为null");
        }

        //  MD5 加盐加密（调用独立工具类，核心步骤）
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16); // 16位随机盐值
        // 加密规则：MD5(明文密码 + 盐值)，调用工具类
//        String encryptedPassword = MD5Util.encrypt(userInfo.getPassword() + salt);
        // 存储格式：加密密码" + salt);

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
    public void updatePostInfo(Post userInfo) {
        if (userInfo == null) {
            throw new BusinessException(400, "更新失败：帖子内容不能为null");
        }
        int affectedRows = postMapper.updatePostInfo(userInfo);
        if (affectedRows != 1) {
            throw new BusinessException(500, "更新失败：数据修改未生效（影响行数：" + affectedRows + "）");
        }
    }
}
