package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Entity.Post;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效

public class PostController {


    //postRequest: http://localhost:12808/lycorisfunServer/api/getPostList
    @GetMapping("/getPostList")
    public List<Post> getPostList(){

        List<Post> posts = new ArrayList<>();
        posts.add(new Post(
                1,
                "Hello, Nice to meet you!",
                "xxxLUCY",
                "hello everyone,i want to show my exciting to meet you, hope we can get on well",
                "2025-10-11 18:07",
                "#/posts/1",
                "https://free.picui.cn/free/2025/10/12/68ea87d94f445.jpg"));

        posts.add(new Post(
                2,
                "How could i change my avater?",
                "Dark Angels",
                "wait…could anyone tell me how could i change my avater? the default one looks not pretty……",
                "2025-10-11 23:17",
                "#/posts/2",
                "https://free.picui.cn/free/2025/10/12/68ea81a34978d.png"
                ));
        posts.add(new Post(
                3,
                "Hello, Nice to meet you!",
                "xxxLUCY",
                "hello everyone,i want to show my exciting to meet you, hope we can get on well",
                "2025-10-11 18:07",
                "#/posts/1",
                "https://free.picui.cn/free/2025/10/12/68ea87d94f445.jpg"));
        posts.add(new Post(
                4,
                "How could i change my avater?",
                "Dark Angels",
                "wait…could anyone tell me how could i change my avater? the default one looks not pretty……",
                "2025-10-11 23:17",
                "#/posts/2",
                "https://free.picui.cn/free/2025/10/12/68ea81a34978d.png"
        ));
        posts.add(new Post(
                5,
                "Hello, Nice to meet you!",
                "xxxLUCY",
                "hello everyone,i want to show my exciting to meet you, hope we can get on well",
                "2025-10-11 18:07",
                "#/posts/1",
                "https://free.picui.cn/free/2025/10/12/68ea87d94f445.jpg"));
        return posts;
    }
}
