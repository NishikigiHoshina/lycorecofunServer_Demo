package com.lycorisfun.fun;

import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class LycorisfunServerApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void test(){
        List<User> userlist=userMapper.findAll();
        for(User user:userlist){
            System.out.println(user);
        }
        System.out.println("查到 " + userlist.size() + " 条");
        userlist.forEach(System.out::println);
    }
}
