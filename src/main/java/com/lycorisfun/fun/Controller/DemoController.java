package com.lycorisfun.fun.Controller;
import com.lycorisfun.fun.Entity.Post;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin
public class DemoController {
    @GetMapping("/getStatu200")
    public Map<String,Object> getStatu200(@RequestParam(value = "num",defaultValue = "1",required = false) Integer num){
        Map<String,Object> map = new HashMap<>();
        map.put("status","200");
        map.put("msg","success");
        int n=3/num;
        return map;


//        try {
//            Map<String,Object> map = new HashMap<>();
//            map.put("status","200");
//            map.put("msg","success");
//            int n=3/num;
//            return map;
//        }catch (Exception e) {
//            e.printStackTrace();
//            Map<String,Object> map = new HashMap<>();
//            map.put("status","500");
//            map.put("msg","failed");
//            return map;
//        }

    }

    @GetMapping("/getStatu500")
    public Map<String,Object> getStatu500(@RequestParam(value = "num",defaultValue = "1",required = false) Integer num){
        Map<String,Object> map = new HashMap<>();
        map.put("status","500");
        map.put("msg","success");
        int n=Integer.parseInt("a");
        return map;
    }


    @GetMapping("/demoGetlist")
    public List<Post> demoGetlist(@RequestParam(value = "num",defaultValue = "1",required = false) Integer num,
                                  @RequestParam(value = "user",defaultValue = "*",required = false) String user){
        List<Post> list = new ArrayList<>();
        list.add(new Post(
                1,
                "Hello, Nice to meet you!",
                2,
                "hello everyone,i want to show my exciting to meet you, hope we can get on well",
                "2025-10-11 18:07:00",
                "#/posts/1",
                "https://free.picui.cn/free/2025/10/12/68ea87d94f445.jpg"));

        list.add(new Post(
                2,
                "How could i change my avater?",
                2,
                "wait…could anyone tell me how could i change my avater? the default one looks not pretty……",
                "2025-10-11 23:17",
                "#/posts/2",
                "https://free.picui.cn/free/2025/10/12/68ea81a34978d.png"
        ));
        Integer id=num;
        List<Post> post=new ArrayList<>();
        for(int i=0;i<list.size();i++){
            if(Objects.equals(id, list.get(i).getPostid())){
                post.add(list.get(i));
            }
        }
        return post;
    }


//    接收前端json数据包请求实现方式
    @PostMapping("/demoAddpost1")
//    public void addPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
//    public Map<String,Object> addPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        public Map<String, Object> addPost(@RequestParam Integer id,
                                           @RequestParam String title,
                                           @RequestParam String content,
                                           @RequestParam int user) {
        Post post = new Post(id, title, user, content);
//        String strid=request.getParameter("id");
//        String title=request.getParameter("title");
//        String main=request.getParameter("content");
//        String user=request.getParameter("user");
//        int id=Integer.parseInt(strid);
//        Post post=new Post(id,title,user,main);
        Map<String,Object> map=new HashMap<>();
        map.put("status","200");
        map.put("success","true");
        map.put("msg","添加成功");
        map.put("post",post);
        return map;
    }


//    接收前端表单实现方式
    @Data
    public class PostDTO {
        @NotNull
        private Integer id;
        @NotBlank
        private String title;
        private String content;
        private int userid;
    }

    @PostMapping("/demoAddpost")
    public Map<String, Object> addPost(@ModelAttribute PostDTO dto) {
        //    public Map<String, Object> addPost(@Valid @RequestBody PostDTO dto) {
        Post post = new Post(dto.getId(), dto.getTitle(), dto.getUserid(), dto.getContent());
        Map<String,Object> map=new HashMap<>();
        map.put("status","200");
        map.put("success","true");
        map.put("msg","添加成功");
        map.put("post",post);
        return map;
    }
}

