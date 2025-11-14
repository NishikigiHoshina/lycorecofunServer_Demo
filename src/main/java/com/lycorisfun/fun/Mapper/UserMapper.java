package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    public List<User> findAll();
    public User findById(int id);

    public int insert(User userinfo);
    public int deletebyid(int id);

    public User findByName(String name);
    public User findBytime(String time);
    public User findByUser(String user);

    public int save(User post);
}
