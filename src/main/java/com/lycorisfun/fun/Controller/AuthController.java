package com.lycorisfun.fun.Controller;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/admin/login")
    public Map<String, Object> login(@RequestBody User user) {

        Map<String, Object> map = new HashMap<>();

        User u = userService.Login(user.getEmail(), user.getPassword());
        if (u != null) {
            Map<String, String> payload = new HashMap<>();
            payload.put("id", u.getUserId().toString());
            payload.put("username", u.getUserName());
            String token = JWTUtil.createToken(payload);

            map.put("code", 200);
            map.put("msg", "登录成功");
            map.put("token", token);
            map.put("userId", u.getUserId());
            map.put("username", u.getUserName());
            map.put("avater",u.getAvaterURL());
            map.put("status", u.getStatus());
        }
        else {
            map.put("code", 400);
            map.put("msg","登录失败");
        }
        return map;
    }

    @PostMapping("/admin/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        if(user!=null){
            User u=new User();
            try {
                u=userService.findByEmail(user.getEmail());
                map.put("code", 400);
                map.put("msg","注册失败,用户已存在");
                return map;
                //throw new BusinessException(405,"用户已注册！");
            }catch (Exception e){
                System.out.println(e);
                userService.add(user);
                map.put("code", 200);
                map.put("msg","注册成功,请前往登录");
                return map;
            }

        }
        else
        {
            map.put("code", 400);
            map.put("msg","注册失败,信息为空");
        }
        return map;
    }
}
