package com.lycorisfun.fun.Entity;

public class function {
    private int id;
    private String function_name;
    private int function_status;
    private  String function_link;

    public function(int id, String function_name, int function_status, String function_link) {
        this.id = id;
        this.function_name = function_name;
        this.function_status = function_status;
        this.function_link = function_link;
    }

    public function(String function_name,int function_status){
        this.function_name = function_name;
        this.function_status = function_status;
    }

    public function() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFunction_status() {
        return function_status;
    }

    public void setFunction_status(int function_status) {
        this.function_status = function_status;
    }

    public String getFunction_name() {
        return function_name;
    }

    public void setFunction_name(String function_name) {
        this.function_name = function_name;
    }

    public String getFunction_link() {
        return function_link;
    }

    public void setFunction_link(String function_link) {
        this.function_link = function_link;
    }

    @Override
    public String toString() {
        return "function{" +
                "id=" + id +
                ", function_name='" + function_name + '\'' +
                ", function_status=" + function_status +
                ", function_link='" + function_link + '\'' +
                '}';
    }
}
