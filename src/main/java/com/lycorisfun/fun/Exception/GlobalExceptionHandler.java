package com.lycorisfun.fun.Exception;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ArithmeticException.class)
    public Map<String,Object> ArithmeticExceptionHandler(ArithmeticException e, HttpServletResponse response){
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        Map<String,Object> map = new HashMap<>();
        map.put("code",500);
        map.put("msg","ArithmeticException-服务器计算错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    @ExceptionHandler(value = NumberFormatException.class)
    public Map<String,Object> NumberFormatExceptionHandler(NumberFormatException e, HttpServletResponse response){
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        Map<String,Object> map = new HashMap<>();
        map.put("code",500);
        map.put("msg","NumberFormatException-服务器格式转换错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    @ExceptionHandler(value = Exception.class)
    public Map<String,Object> OtherExceptionHandler(Exception e, HttpServletResponse response){
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        Map<String,Object> map = new HashMap<>();
        map.put("code",500);
        map.put("msg","OtherException-服务器其他错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    /**
     * 业务异常：把 body 里的业务 code 同时映射为 HTTP 状态码，
     * 使 400/401/403/404 等在传输层也是对应状态（前端仍以 body.code 判成功）。
     */
    @ExceptionHandler(value = BusinessException.class)
    public Map<String,Object> ServiceExceptionHandler(BusinessException e, HttpServletResponse response){
        int code = e.getCode();
        int httpStatus = (code >= 400 && code < 600) ? code : HttpStatus.BAD_REQUEST.value();
        response.setStatus(httpStatus);
        Map<String,Object> map = new HashMap<>();
        map.put("code",code);
        map.put("msg","ServiceException-业务类错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Map<String,Object> handle404(NoResourceFoundException ex, HttpServletResponse response){
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return Map.of("code",404,"msg","接口不存在");
    }
}
