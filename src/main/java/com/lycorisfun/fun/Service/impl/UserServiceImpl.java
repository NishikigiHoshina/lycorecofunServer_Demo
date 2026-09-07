package com.lycorisfun.fun.Service.impl;


import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.Md5SaltUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> findAll() {
        //已实现方法
        List<User> userList = userMapper.findAll();
        if (userList == null || userList.isEmpty()) {
            // 抛业务异常，状态码 404
            throw new BusinessException(404, "查询失败：未找到任何用户数据");
        }
        return userList;
    }



    @Override
    public void deletebyid(Integer id) {
        //已实现方法
        if (id == null || id < 0) {
            throw new BusinessException(400, "删除失败：用户ID无效（不能为null或负数或0）");
        }
        int affectedRows = userMapper.deletebyid(id);
        if (affectedRows == 0) {
            throw new BusinessException(404, "删除失败：ID为" + id + "的用户不存在");
        }
        System.out.println("删除成功!影响:"+affectedRows+"行");
    }


    @Override
    public void add(User userInfo) {
        if (userInfo == null) {
            throw new BusinessException(400, "新增失败：用户信息不能为null");
        }
        //  MD5 加盐加密（调用独立工具类，核心步骤）
        String encryptedPassword=Md5SaltUtil.encrypt(userInfo.getPassword());
        userInfo.setPassword(encryptedPassword);
        userInfo.setRegisterTime(LocalDateTime.now().toString());
        int affectedRows = userMapper.insert(userInfo);
        if (affectedRows != 1) {
            throw new BusinessException(500, "新增失败：插入数据未生效（影响行数：" + affectedRows + "）");
        }

    }


    @Override
    public User findById(Integer id) {
        //已实现方法
        if (id == null || id <= 0) {
            throw new BusinessException(400, "查询失败：用户ID无效（不能为null或负数或0）");
        }
        User userInfo = userMapper.findById(id);
        if (userInfo == null) {
            throw new BusinessException(404, "查询失败：ID为" + id + "的用户不存在");
        }
        // 数据脱敏（密码置空，避免返回给前端）
        userInfo.setPassword(null);

        return userInfo;
    }

    @Override
    public List<User> findByUsername(String userName) {
        //已实现方法
        if (userName == null ) {
            throw new BusinessException(400, "查询失败：用户名无效（不能为null）");
        }
        List<User> userInfoList = userMapper.findByUsername(userName);
        if (userInfoList == null) {
            throw new BusinessException(404, "查询失败：ID为" + userName + "的用户不存在");
        }
        // 数据脱敏（密码置空，避免返回给前端）
//      userInfoList.setPassword(null);

        return userInfoList;
    }




    @Override
    public void save(User userInfo) {
        if (userInfo == null) {
            throw new BusinessException(400, "更新失败：用户信息不能为null");
        }
        int affectedRows = userMapper.save(userInfo);
        if (affectedRows != 1) {
            throw new BusinessException(500, "更新失败：数据修改未生效（影响行数：" + affectedRows + "）");
        }
    }

    @Override
    public User findByEmail(String email){
        if (email == null) {
            throw new BusinessException(400,"Bad Request,请求参数错误");
        }
        User u=userMapper.findByEmail(email);
        if (u == null) {
            throw new BusinessException(404,"数据库中无数据");
        }
        return u;
    }


    @Override
    public User Login(String email, String password) {
        if (email == null) {
            throw new BusinessException(400,"Bad Request,邮箱为空");
        }
        if (password == null) {
            throw new BusinessException(400,"Bad Request,密码为空");
        }
        User u=userMapper.findByEmail(email);
        if (u == null) {
            throw new BusinessException(404,"该用户未注册");
        }
        String dbPassword = u.getPassword();
        boolean allowLogin = Md5SaltUtil.verify(password,dbPassword);
        if (allowLogin) {
            return u;
        }else {
            throw new BusinessException(400,"用户名或密码错误");
        }
    }

    @Override
    public User updateUserInfo(User user){
        if (user == null) {
            throw new BusinessException(400,"修改失败，未查找到用户");
        }
        User u=new User();
        if(user.getUserId()==0){
            throw new BusinessException(400,"修改失败，未查找到用户");
        }
        u.setUserId(user.getUserId());//通过id匹配用户对象
        if(user.getUserName()!=null){
            u.setUserName(user.getUserName());
        }
        if(user.getPassword()!=null){
            u.setPassword(user.getPassword());
        }
        if(user.getEmail()!=null){
            u.setEmail(user.getEmail());
        }
        if(user.getGender()!=null){
            u.setGender(user.getGender());
        }
        if(user.getSignature()!=null){
            u.setSignature(user.getSignature());
        }
        if(user.getAvaterURL()!=null){
            u.setAvaterURL(user.getAvaterURL());
        }
        if(user.getPersonalIndexLink()!=null){
            u.setPersonalIndexLink(user.getPersonalIndexLink());
        }
        int n=userMapper.updateUserInfo(u);      // 实际只生成 SET email=?, WHERE id=12
        System.out.println("修改成功，影响:"+n+"行");
        return u;
    }

    @Override
    public User findProfileById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "查询失败：用户ID无效");
        }
        // SQL 层已排除 password，此处无需再脱敏
        User u = userMapper.findProfileById(id);
        if (u == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return u;
    }
}
