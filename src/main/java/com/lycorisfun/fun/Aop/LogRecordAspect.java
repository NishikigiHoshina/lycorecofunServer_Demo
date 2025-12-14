package com.lycorisfun.fun.Aop;

import com.lycorisfun.fun.Entity.LogRecord;
import com.lycorisfun.fun.Mapper.LogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class LogRecordAspect {
    @Autowired
    private LogMapper logMapper;

    @After("execution(* com.lycorisfun.fun.Service.*.*(..))")
    public void afterLog(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getName();
        String classmethod = joinPoint.getSignature().getName();
        LocalDateTime time = LocalDateTime.now();
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) ra).getRequest();
        String ip = getIpAddr(request);
        String httpmethod = request.getMethod();

        LogRecord logRecord = new LogRecord(ip, time.toString(),className, classmethod,httpmethod,1);
        //logMapper.insertLog(logRecord);
    }

    //常用 IP 获取算法（考虑 X-Forwarded-For、X-Real-IP）
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时 X-Forwarded-For 第一个逗号前为真实 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip;
    }
}
