package com.lycorisfun.fun.Interceptor;

import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.JWTUtil;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

//@Component注解表示在spring容器中创建对象
//@Component注解有3个子注解，分别为：@Controller、@Service、@Repository
@Component
public class LoginInterceptor implements HandlerInterceptor {

    //进入方法前被执行
    //登录拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        System.out.println("handler:" + handler);
        if(!(handler instanceof HandlerMethod)) {//当前处理请求的对象不是方法
            return true;//放行
        }
        String token = request.getHeader("token");
        System.out.println("LoginInterceptor token=" + token);

        Map<String, Object> map = new HashMap<>();
        if(token == null || token=="" || "null".equals(token)) {
            map.put("code", 401);
            map.put("msg", "未登录");
            String jsonstring = new Gson().toJson(map);//将对象转化为json格式
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(jsonstring);
            return false;//拦截了
        }

        try {
            JWTUtil.verifyToken(token);
            return true;//放行
        } catch (SignatureVerificationException e) {
            e.printStackTrace();
            map.put("code", 400);
            map.put("msg", "签名不正确");
        } catch (AlgorithmMismatchException e) {
            e.printStackTrace();
            map.put("code", 400);
            map.put("msg", "签名算法不正确");
        } catch (TokenExpiredException e) {
            e.printStackTrace();
            map.put("code", 400);
            map.put("msg", "token过期了");
        } catch (Exception e) {
            e.printStackTrace();
            map.put("code", 400);
            map.put("msg", "发生异常");
        }
        String jsonstring = new Gson().toJson(map);
        response.setContentType("application/json;charset=utf-8");//将对象转化为json格式
        response.getWriter().write(jsonstring);
        return false;//拦截了
    }
}