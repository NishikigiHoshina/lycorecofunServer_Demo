package com.lycorisfun.fun.Entity;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private Integer id;
    private String user;
    private String main;
    private String time;
    private String title;
    private String link;
    private String imgurl;
    private List<String> tag = new ArrayList<>();

    public Post() {
    }

    public Post(Integer id,String title, String user, String main, String time,  String link, String imgurl, List<String> tag) {
        this.id = id;
        this.user = user;
        this.main = main;
        this.time = (time != null) ? time : "1970-1-1";
        this.title = title;
        this.link = (link != null) ? link : "#/";
        this.imgurl = (imgurl != null) ? imgurl : "";
        this.tag = (tag != null) ? tag : new ArrayList<>();
    }

    public Post(Integer id, String title, String user, String main,
                String time, String link, String imgurl) {
        this(id, title, user, main, time, link, imgurl, new ArrayList<>()); // 委托给全参
    }

    public Post(Integer id, String title, String user, String main){
        this(id, title, user, main, "" , "", "", new ArrayList<>());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getImgurl() {
        return imgurl;
    }
    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }

    public List<String> getTag() {
        return tag;
    }
    public void setTag(List<String> tag) {
        this.tag = (tag != null) ? tag : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", user='" + user + '\'' +
                ", main='" + main + '\'' +
                ", time='" + time + '\'' +
                ", title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", imgurl='" + imgurl + '\'' +
                ", tag=" + tag +
                '}';
    }
}
