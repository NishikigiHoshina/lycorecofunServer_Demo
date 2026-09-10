package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Interceptor.TokenInterceptor;
import com.lycorisfun.fun.Service.FileStorageService;
import com.lycorisfun.fun.Service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）

public class PostController {

    @Autowired
    PostService postService;

    @Autowired
    private FileStorageService fileStorage;

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
        // 列表的 content 已由 findPageList 规整为纯文本摘要（≤255 字），此处不再清空：
        // 广场卡片需要它作为预览。正文文档不在列表接口返回。
        List<Post> posts = postService.findPageList(page, size);
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

    /**
     * 帖子正文配图上传：**任何登录用户**都可调用（作者要能插图）。
     *
     * <p>这是本次改造中唯一放宽的上传权限点（站内宣传图仍仅管理员）。因此这里刻意用
     * {@code storeImage()} 而非 {@code store()}：解码后重新编码再落盘，可丢掉 EXIF 与
     * 追加在文件尾部的载荷。配套还有类型白名单、魔数校验、大小与像素上限、uuid 命名。</p>
     *
     * <p>返回的是**相对 URL**（如 {@code /upload/post/xxx.png}）——正文文档里存相对路径，
     * 换域名/部署环境时不失效，渲染时由前端补 origin。</p>
     */
    @PostMapping("/uploadPostImg")
    @RequireToken
    public Map<String, Object> uploadPostImg(MultipartFile file, HttpServletRequest request) {
        Integer callerId = TokenInterceptor.currentUserId(request);
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        String url = fileStorage.storeImage(file, "post-img");
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "上传成功");
        map.put("url", url);
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
