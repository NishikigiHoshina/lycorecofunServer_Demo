package com.lycorisfun.fun.Entity;

public class News {
    private int id;
    private String title;
    private String content;
    private String author;
    private String date;
    private String imgurl;
    private String link;
    public News(int id, String title, String content, String author, String date, String imgurl, String link) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.date = date;
        this.imgurl = imgurl;
        this.link = link;
    }

    public News(int id,String title, String imgurl, String link) {
        this.id = id;
        this.title = title;
        this.imgurl = imgurl;
        this.link = link;
        this.author="未知";
        this.content="无";
        this.date="beyondTime";

    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
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
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getImgurl() {
        return imgurl;
    }
    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }


}
