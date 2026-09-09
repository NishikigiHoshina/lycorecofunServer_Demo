package com.lycorisfun.fun.Entity;

public class News {
    private int news_id;
    private String title;
    private String content;
    private String author;
    private String time;
    private String imgurl;
    private String news_link;
    private int status = 1;   // 显示状态：1=显示(默认)，0=软删除
    public News(int news_id, String title, String content, String author, String time, String imgurl, String news_link) {
        this.news_id = news_id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.time = time != null ? time : "BeyondtheTime" ;
        this.imgurl = imgurl;
        this.news_link =  news_link;
    }

    public News(int news_id, String title, String imgurl, String news_link,String time) {
        this.news_id = news_id;
        this.title = title;
        this.imgurl = imgurl;
        this.news_link = news_link;
        this.author="未知";
        this.content="无";
        this.time =time != null ? time : "BeyondtheTime" ;

    }

    public News() {

    }

    public int getNews_id() {
        return news_id;
    }
    public void setNews_id(int news_id) {
        this.news_id = news_id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public String getImgurl() {
        return imgurl;
    }
    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }
    public String getNews_link() {
        return news_link;
    }
    public void setNews_link(String news_link) {
        this.news_link = news_link;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

}
