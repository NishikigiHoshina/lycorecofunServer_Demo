package com.lycorisfun.fun.Service.impl;


import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.Md5SaltUtil;
import com.lycorisfun.fun.util.TextValidator;
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
        // 数据脱敏：password 置空，避免列表接口外泄加盐密文
        for (User u : userList) {
            u.setPassword(null);
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
        // 入库前字符校验（库 utf8mb3，拦截 emoji 等 4 字节字符）
        TextValidator.requireStorable(userInfo.getUserName(), "用户名");
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
        for (User u : userInfoList) {
            u.setPassword(null);
        }

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

    /**
     * 后台用户管理：更新用户资料（调用方须已通过管理员校验）。
     * 入参是"部分更新"语义——某字段为 null 表示不改该列（对应 XML 里的 if test="x != null"）。
     * 三处与旧实现不同的关键点：
     *   1. password 传了才改，且必须加盐哈希后落库（旧实现直接存明文）；
     *   2. email 有 UNIQUE 约束，改成已被占用的邮箱先查重给友好 400，而不是撞库变 500；
     *   3. status 是原始 int，XML 的 if test="status != null" 对 int 恒真 → 一定会写库，
     *      所以这里必须挡掉非法值（0 视为"未提供"，保留原状态）。
     */
    @Override
    public User updateUserInfo(User user){
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(400,"修改失败：缺少用户ID");
        }
        int targetId = user.getUserId();
        // 目标用户必须存在（ID 无效/不存在时由此抛 400/404）
        User existing = findById(targetId);

        // 入库前字符校验（库 utf8mb3，拦截 emoji 等 4 字节字符）
        TextValidator.requireStorable(user.getUserName(), "用户名");
        TextValidator.requireStorable(user.getSignature(), "签名");
        TextValidator.requireStorable(user.getAvaterURL(), "头像链接");
        TextValidator.requireStorable(user.getPersonalIndexLink(), "个人主页链接");

        User u=new User();
        u.setUserId(targetId);//通过id匹配用户对象
        if(user.getUserName()!=null){
            u.setUserName(user.getUserName());
        }
        if(user.getPassword()!=null && !user.getPassword().isEmpty()){
            u.setPassword(Md5SaltUtil.encrypt(user.getPassword()));  // 必须加盐哈希，绝不存明文
        }
        if(user.getEmail()!=null && !user.getEmail().equals(existing.getEmail())){
            // email 列有 UNIQUE 约束：先查占用，避免插入时抛约束异常变成 500
            User owner = null;
            try {
                owner = userMapper.findByEmail(user.getEmail());
            } catch (BusinessException notFound) {
                // findByEmail 未命中会抛 404，说明该邮箱可用
            }
            if (owner != null && !owner.getUserId().equals(targetId)) {
                throw new BusinessException(400,"修改失败：邮箱已被其他用户使用");
            }
            u.setEmail(user.getEmail());
        }
        if(user.getGender()!=null){
            u.setGender(user.getGender());
        }
        if(user.getRegisterTime()!=null){
            u.setRegisterTime(user.getRegisterTime());
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
        // status：0 = 未提供 → 保留原状态；合法值只有 1/2/3（UserMapper.xml 里 status 恒更新）
        int status = user.getStatus();
        if (status == 0) {
            status = existing.getStatus();
        } else if (status != 1 && status != 2 && status != 3) {
            throw new BusinessException(400,"修改失败：状态只能为 1(正常)/2(停用)/3(管理员)");
        }
        u.setStatus(status);

        int n=userMapper.updateUserInfo(u);
        if (n != 1) {
            throw new BusinessException(500,"修改失败：数据修改未生效（影响行数："+n+"）");
        }
        System.out.println("修改成功，影响:"+n+"行");
        u.setPassword(null);     // 返回体不带密文
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
