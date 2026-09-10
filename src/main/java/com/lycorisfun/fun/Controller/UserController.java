package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.User;

import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/searchUser")
    public User searchUser(@RequestParam(value = "id",defaultValue = "1",required = false) Integer id,
                           @RequestParam(value = "name",defaultValue = "未知用户",required = false) String name) {
    //public String home() {
        System.out.println("接收到前端请求，开始处理");
        User user = new User();
        user.setUserId(id);
        user.setUserName(name);
        System.out.println("id:" + id);
        System.out.println("name:" + name);
        return user;
    }

    @PostMapping("/searchByUsername")
    public List<User> searchByUsername(@RequestParam(value = "name",defaultValue = "unknown",required = true) String name){
        List<User> users = new ArrayList<>();
        users=userService.findByUsername(name);
        return users;
    }

    @PostMapping("/searchByuserid")
    public User searchByUserId(@RequestParam(value = "id",defaultValue = "1",required =true) Integer id){
        User users = new User();
        users=userService.findById(id);
        return users;
    }

    @GetMapping("/userlist")
    public List<User> userlist() {
        List<User> users = userService.findAll();
        if(users==null){
            throw new BusinessException(404,"找不到数据");
        }
        return users;
    }

    // 个人中心：返回当前登录用户信息（password 不在查询列中）
    // 需登录：@RequireToken；userId 由 TokenInterceptor 从 JWT 解析后放入 request 属性
    @PostMapping("/getMyProfile")
    @com.lycorisfun.fun.Annotation.RequireToken
    public Map<String, Object> getMyProfile(HttpServletRequest request) {
        String userIdStr = (String) request.getAttribute("userId");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new BusinessException(401, "未登录");
        }
        int userId = Integer.parseInt(userIdStr);
        User u = userService.findProfileById(userId);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", u);
        return map;
    }

    // 后台"用户管理"编辑对话框：更新用户资料（系统级写操作，仅管理员）
    // 返回落库后的用户（不含 password），供前端就地刷新表格行
    @PostMapping("/updateUserinfo")
    @RequireToken
    public Map<String, Object> updateUserinfo(@RequestBody User user, HttpServletRequest request) {
        AuthUtil.requireAdmin(request, userMapper);
        User updated = userService.updateUserInfo(user);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "保存成功");
        map.put("data", updated);
        return map;
    }

    // 后台"用户管理"删除用户：软删除（users.status 置 2 停用），仅管理员。
    // 注意与帖子的 /deletePost 区分：此前前端误用帖子端点，会删掉同号帖子。
    @PostMapping("/deleteUser")
    @RequireToken
    public Map<String, Object> deleteUser(@RequestParam("userid") Integer userid,
                                         HttpServletRequest request) {
        AuthUtil.requireAdmin(request, userMapper);
        userService.deletebyid(userid);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "已停用该用户");
        return map;
    }

}
