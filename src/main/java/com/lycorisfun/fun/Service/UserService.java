package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.User;

import java.util.List;

public interface UserService {
    public List<User> findAll();

    public void deletebyid(Integer id);

    public void add(User userInfo);

    public User findById(Integer id);

    public void save(User userInfo);

}

