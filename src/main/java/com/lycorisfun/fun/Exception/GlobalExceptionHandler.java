package com.lycorisfun.fun.Exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = ArithmeticException.class)
    public Map<String,Object> ArithmeticExceptionHandler(ArithmeticException e){
        Map<String,Object> map = new HashMap<>();
        map.put("code",500);
        map.put("msg","ArithmeticException-服务器计算错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = NumberFormatException.class)
    public Map<String,Object> NumberFormatExceptionHandler(NumberFormatException e){
        Map<String,Object> map = new HashMap<>();
        map.put("code",500);
        map.put("msg","NumberFormatException-服务器格式转换错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public Map<String,Object> OtherExceptionHandler(Exception e){
        Map<String,Object> map = new HashMap<>();
        map.put("code",500);
        map.put("msg","OtherException-服务器其他错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = BusinessException.class)
    public Map<String,Object> ServiceExceptionHandler(BusinessException e){
        Map<String,Object> map = new HashMap<>();
        map.put("code",e.getCode());
        map.put("msg","ServiceException-业务类错误-"+e.getMessage());
        map.put("data",e.toString());
        return map;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Map<String,Object> handle404(NoResourceFoundException ex){
        return Map.of("code",404,"msg","接口不存在");
    }
}
