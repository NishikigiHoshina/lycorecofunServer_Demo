package com.lycorisfun.fun.Exception;


//自定义异常
public class BusinessException extends RuntimeException{
    private int code;
    private String msg;

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
