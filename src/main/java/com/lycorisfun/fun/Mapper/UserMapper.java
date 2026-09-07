package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    public List<User> findAll();
    public User findById(int id);

    public int insert(User userinfo);
    public int deletebyid(int id);

    List<User> findByUsername(@Param("userName") String userName);

    public User findBytime(String time);
    public User findByUser(String user);

    public int save(User post);

    public int updateUserInfo(User u);

    public User findByEmail(String email);

    /* 个人中心：显式列出非敏感列，password 不查询 */
    public User findProfileById(int id);
}
