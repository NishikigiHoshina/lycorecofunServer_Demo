package com.lycorisfun.fun.Entity;

public class LogRecord {
    private Integer id;
    private String IPAddress;
    private String time;
    private String className;
    private String classMethod;
    private String httpMethod;

    public LogRecord() {
    }


    public LogRecord(String IPAddress, String time, String className, String classMethod, String httpMethod, Integer id) {
        this.IPAddress = IPAddress;
        this.time = time;
        this.className = className;
        this.classMethod = classMethod;
        this.httpMethod = httpMethod;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public void setIPAddress(String IPAddress) {
        this.IPAddress = IPAddress;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassMethod() {
        return classMethod;
    }

    public void setClassMethod(String classMethod) {
        this.classMethod = classMethod;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }


    @Override
    public String toString() {
        return "LogRecord{" +
                "IPAddress='" + IPAddress + '\'' +
                ", time='" + time + '\'' +
                ", className='" + className + '\'' +
                ", classMethod='" + classMethod + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", id=" + id +
                '}';
    }
}
