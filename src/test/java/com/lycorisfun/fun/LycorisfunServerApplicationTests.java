package com.lycorisfun.fun;

import com.lycorisfun.fun.Controller.AuthController;
import com.lycorisfun.fun.Entity.News;
import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Entity.function;
import com.lycorisfun.fun.Mapper.FuncMapper;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.NewsService;
import com.lycorisfun.fun.Service.PostService;
import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.Md5SaltUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
class LycorisfunServerApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private FuncMapper funcMapper;

    @Autowired
    private AuthController authController;


    @Test
    public void test(){
        //用户列表查询测试
        List<User> userlist=userMapper.findAll();
        for(User user:userlist){
            System.out.println(user);
        }
        System.out.println("查到 " + userlist.size() + " 条");
        userlist.forEach(System.out::println);
    }

    @Test
    public void test_addUser(){
        //添加新用户测试
        User user=new User("test3","test3@mail.com","1234567");
        //管理员密码youwillneverknow7
        userService.add(user);
    }

    @Test
    public void test_findUserById(){
        //id查找测试
        User user=new User();
        user=userService.findById(1);
        System.out.println(user);
    }

    @Test
    public void test_findUserByName(){
        //用户名查找测试
        List<User> list=userService.findByUsername("test");
        for(User user:list){
            System.out.println(user);
        }
    }

    @Test
    public void test_postlist(){
        //帖子列表查询测试
        List<Post> postList=postService.findAll();
        for(Post post:postList){
            System.out.println(post);
        }
    }

    @Test
    public void test_deletebyid(){
        //用户软删除测试
        userService.deletebyid(3);
    }

    @Test
    public void test_updateUserinfo(){
        //更新用户信息测试
        User user=new User(5,"test7",null,null);
        User u=userService.updateUserInfo(user);
        System.out.println(u);
    }

    @Test
    public void test_findPostByUsername(){
        List<Post> postList=new ArrayList<Post>();
        postList=postService.findByUser("Test");
        for(Post post:postList){
            System.out.println(post);
        }
    }

    @Test
    public void test_findPostByTitle(){
        List<Post> list=postService.findByTitle("h");
        for(Post post:list){
            System.out.println(post);
        }
    }

    @Test
    public void test_findAnnoncement(){
        System.out.println(newsService.findAnnouncement());
    }

    @Test
    public void test_updatePostinfo(){
        //更新用户信息测试
        Post post=new Post(4,"test2",4,null);
        Post u=postService.updatePostInfo(post);
        System.out.println(u);
    }

    @Test
    public void test_newslist(){
        List<News> newsList=newsService.findLatestNews(10);
        for(News news:newsList){
            System.out.println(news);
        }
    }

    @Test
    public void test_findfunc(){
        function func=funcMapper.funcStatus("uploadWork");
        System.out.println(func.getFunction_status());

    }

    @Test
    public void test_funclist(){
        List<function> funclist=funcMapper.listFuncStatus();
        for(function func:funclist){
            System.out.println(func);
        }
    }

    @Test
    public void test_verifyPassword(){
        String password="walnutisgod";
        String dbpassword= Md5SaltUtil.encrypt(password);
        System.out.println(dbpassword);
        if(Md5SaltUtil.verify(password,dbpassword)){
            System.out.println("密码验证成功");
        }
        else {
            System.out.println("密码验证失败");
        }
        //测试账号密码皆为123456，超级管理员密码为walnutisgod
    }

    @Test
    public void test_login(){
        User u=new User();
        u=userService.Login("test@mail.com","123456");
        if(u!=null){
            System.out.println(u);
        }
        else {
            System.out.println("登录失败");
        }
    }

    @Test
    public void test_JWTUtil(){
        User u=new User("test","test@mail.com","123456");
        Map<String, Object> map=authController.login(u);
        System.out.println(map);
    }
}
