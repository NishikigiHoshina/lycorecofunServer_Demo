package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Service.PostService;
import com.lycorisfun.fun.VO.PostListVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效

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
        List<Post> posts=postService.findlist(10);
        return posts;
    }

    @PostMapping("/getPostByid")
    public Post getPostById(@RequestBody Integer postid){
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
    public void updatePostinfo(@Valid @RequestBody Post post ){
        System.out.println(post);
        if(post.getPostid()>0){
            postService.updatePostInfo(post);
        }
    }

    @PostMapping("/deletePost")
    public void deletePost(@RequestParam Integer postid){
        if(postid>0){
            System.out.println("影响"+postService.delById(postid)+"行");
        }
    }

    @PostMapping("/writepost")
    public boolean writePost(@Valid @RequestBody Post post ){
        if(post==null){
            throw new BusinessException(400,"参数错误，新增失败");
        }
        postService.add(post);
        System.out.println("新增成功");
        return true;
    }
}
