package com.lycorisfun.fun;

import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Mapper.PostMapper;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.PostService;
import com.lycorisfun.fun.Service.UserService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class LycorisfunServerApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

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
}
