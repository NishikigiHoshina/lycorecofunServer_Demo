package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Interceptor.TokenInterceptor;
import com.lycorisfun.fun.Service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）

public class PostController {

    @Autowired
    PostService postService;

    //postRequest: http://localhost:12808/lycorisfunServer/api/getPostList
    @GetMapping("/getPostList")
    public List<Post> getPostList(){
        List<Post> posts;
        posts=postService.findAll();
        return posts;
    }

    @PostMapping("/postlist")
    public List<Post> postList(){
        List<Post> posts=postService.findlist(50);
        for (Post post : posts) {
            post.setContent("");
        }
        return posts;
    }

    // 分页查询帖子列表（与 /postlist 同口径：status=1 且 title 非空）
    // 请求：POST /api/postlistPage?page=1&size=10
    // 响应：{ code, msg, list, total, page, size }
    @PostMapping("/postlistPage")
    public Map<String, Object> postlistPage(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        List<Post> posts = postService.findPageList(page, size);
        for (Post post : posts) {
            post.setContent("");          // 列表视图不返回正文，与 /postlist 保持一致
        }
        int total = postService.countPostList();
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("list", posts);
        map.put("total", total);
        map.put("page", page);
        map.put("size", size);
        return map;
    }

    @PostMapping("/getPostByid")
    public Post getPostById(@RequestParam("postid") Integer postid){
        Post post=postService.findById(postid);
        return post;
    }

    @PostMapping("/searchBytitle")
    public List<Post> searchByTitle(@RequestParam String title){
        List<Post> posts;
        posts=postService.findByTitle(title);
        return posts;
    }

    @PostMapping("/searchByuser")
    public List<Post> searchByUser(@RequestParam String username){
        List<Post> posts;
        posts=postService.findByUser(username);
        return posts;
    }

    @PostMapping("/updatePostinfo")
    @RequireToken
    public void updatePostinfo(@Valid @RequestBody Post post, HttpServletRequest request ){
        System.out.println(post);
        Integer callerId = TokenInterceptor.currentUserId(request);
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        if(post.getPostid()!=null && post.getPostid()>0){
            postService.updatePostInfo(post, callerId);
        } else {
            throw new BusinessException(400,"参数错误：帖子ID无效");
        }
    }


    @PostMapping("/deletePost")
    @RequireToken
    public void deletePost(@RequestParam Integer postid, HttpServletRequest request){
        Integer callerId = TokenInterceptor.currentUserId(request);
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        if(postid!=null && postid>0){
            System.out.println("影响"+postService.delById(postid, callerId)+"行");
        } else {
            throw new BusinessException(400,"参数错误：帖子ID无效");
        }
    }

    @PostMapping("/takemessage")
    public  boolean takemessage(@RequestBody Post post){
        if(post==null){
            throw new BusinessException(400,"参数错误，新增失败");
        }
        postService.addmessage(post);
        System.out.println("新增成功");
        return true;
    }

    @PostMapping("/writepost")
    @RequireToken
    public Map<String, Object> writePost(@Valid @RequestBody Post post, HttpServletRequest request ){
        Map<String, Object> map = new HashMap<>();
        if(post==null){
            throw new BusinessException(400,"参数错误，新增失败");
        }
        Integer callerId = TokenInterceptor.currentUserId(request);
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        post.setPost_userid(callerId);          // 作者身份以登录态为准，忽略 body 提交值（防伪造）
        postService.add(post);
        System.out.println("新增成功");
        map.put("code", 200);
        map.put("msg","发布成功");
        return map;
    }

    @PostMapping("/writecomment")
    @RequireToken
    public Map<String,Object> writeContent(@Valid @RequestBody Post post, HttpServletRequest request ){
        Map<String, Object> map = new HashMap<>();
        if(post==null){
            throw new BusinessException(400,"参数错误，新增失败");
        }
        Integer callerId = TokenInterceptor.currentUserId(request);
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        post.setPost_userid(callerId);          // 评论/回复作者以登录态为准，忽略 body 提交值（防伪造）
        postService.addcontent(post);
        System.out.println("新增成功");
        map.put("code", 200);
        map.put("msg","发布成功");
        return map;
    }

    @PostMapping("/getReply")
    public Map<String,Object> getReply(@RequestParam Integer parent_id){
        List<Post>list=postService.findContentPointaPost(parent_id);
        Map<String, Object> map = new HashMap<>();
        if(list!=null && list.size()>0){
            map.put("code", 200);
            map.put("msg","查询成功");
            map.put("replylist",list);
        }else {
            map.put("code", 404);
            map.put("msg","查询失败");
        }
        return map;
    }
}
