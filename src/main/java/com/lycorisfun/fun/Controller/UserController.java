package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Entity.User;

import com.lycorisfun.fun.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

import java.io.IOException;
import java.util.*;

@RestController
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    private final HttpRequestHandlerAdapter httpRequestHandlerAdapter;
    private final HttpServletResponse httpServletResponse;

    public UserController(HttpRequestHandlerAdapter httpRequestHandlerAdapter, HttpServletResponse httpServletResponse) {
        this.httpRequestHandlerAdapter = httpRequestHandlerAdapter;
        this.httpServletResponse = httpServletResponse;
    }

    @GetMapping("/searchUser")
    public User searchUser(@RequestParam(value = "id",defaultValue = "1",required = false) Integer id,
                           @RequestParam(value = "name",defaultValue = "未知用户",required = false) String name) {
    //public String home() {
        System.out.println("接收到前端请求，开始处理");
        User user = new User();
        user.setUserId(id);
        user.setUserName(name);
//        System.out.println("home");
        System.out.println("id:" + id);
        System.out.println("name:" + name);
        //int a = 7 / 0;
        return user;
    }

    @PostMapping("/user")
    public String user(@RequestBody User user) {
        System.out.println("user");
        System.out.println(user.toString());
        return null;
    }

    @GetMapping("/userlist")
    public List<User> userlist() {
        List<User> users = new ArrayList<>();
        users=userService.findAll();
        return users;
    }

//    @GetMapping("/httpStatusDemo")
//    public void demo(HttpServletResponse resp) throws IOException {
//        resp.setStatus(HttpServlet.SC_NO_CONTENT); // 204
//
//    }

    @GetMapping("/getStatus")
    public Map<String, Object> getStatus() {
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("statu", 418);
        map.put("msg","I'm a teapot");
        map.put("whatisthis", "别担心！状态码 418 实际上是一个愚人节玩笑。它在 RFC 2324 中定义，该 RFC 是一个关于超文本咖啡壶控制协议（HTCPCP）的笑话文件。在这个笑话中，418 状态码是作为一个玩笑加入到 HTTP 协议中的");
        return map;
    }


    //前端请求： http://localhost:12808/lycorisfunServer/getgood01?id=1&name=apple
    @RequestMapping("/getgood01")
    public void getgood01(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String strid = request.getParameter("id");
        String name=request.getParameter("name");
        int id= Integer.parseInt(strid);
        String str="id:"+id+",name:"+name;
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(str);
    }

}
