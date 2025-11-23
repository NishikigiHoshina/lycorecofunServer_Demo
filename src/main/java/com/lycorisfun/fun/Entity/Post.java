package com.lycorisfun.fun.Entity;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private Integer postid;
    private int post_userid;
    private String userName;
    private String content;
    private String created_at;
    private String title;
    private String link;
    private String imgurl;
    private List<String> tag = new ArrayList<>();
    private int like_count;
    private int reply_count;
    private int root_id;
    private int parent_id;
    private int status;

    public Post() {
    }

    public Post(Integer postid, String title, int post_userid, String userName, String content, String created_at, String link, String imgurl, List<String> tag, int like_count, int reply_count, int root_id, int parent_id, int status) {
        this.postid = postid;
        this.post_userid = post_userid;
        this.userName = userName != null ? userName : "";
        this.content = content;
        this.created_at = (created_at != null) ? created_at : "1970-1-1 00:00:00";
        this.title = title;
        this.link = (link != null) ? link : "#/";
        this.imgurl = (imgurl != null) ? imgurl : "";
        this.tag = (tag != null) ? tag : new ArrayList<>();
        this.like_count = (like_count != 0) ? like_count : 0;
        this.reply_count = (reply_count != 0) ? reply_count : 0;
        this.root_id = (root_id != 0) ? root_id : 0;
        this.parent_id = (parent_id != 0) ? parent_id : 0;
        this.status = (status != 0) ? status : 0;
    }
    //全参构造方法

    public Post(Integer postid, String title, int post_userid, String content,
                String created_at, String link, String imgurl) {
        this(postid, title, post_userid,"", content, created_at, link, imgurl, new ArrayList<>(),0,0,0,0,0); // 委托给全参
    }

    public Post(Integer postid, String title, int post_userid, String content){
        this(postid, title, post_userid,"", content, "" , "", "", new ArrayList<>(),0,0,0,0,0);// 委托给全参
    }

    public Integer getPostid() {
        return postid;
    }

    public void setPostid(Integer postid) {
        this.postid = postid;
    }

    public int getPost_userid() {
        return post_userid;
    }

    public void setPost_userid(int post_userid) {
        this.post_userid = post_userid;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
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
    public int getLike_count() {
        return like_count;
    }
    public void setLike_count(int like_count) {
        this.like_count = like_count;
    }
    public int getReply_count() {
        return reply_count;
    }
    public void setReply_count(int reply_count) {
        this.reply_count = reply_count;
    }
    public int getRoot_id() {
        return root_id;
    }
    public void setRoot_id(int root_id) {
        this.root_id = root_id;
    }
    public int getParent_id() {
        return parent_id;
    }
    public void setParent_id(int parent_id) {
        this.parent_id = parent_id;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "Post{" +
                "id=" + postid +
                ", userid='" + post_userid + '\'' +
                ", username='" + userName + '\'' +
                ", main='" + content + '\'' +
                ", time='" + created_at + '\'' +
                ", title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", imgurl='" + imgurl + '\'' +
                ", tag=" + tag +'\'' +
                ", like_count=" + like_count +'\'' +
                ", reply_count=" + reply_count +'\'' +
                ", root_id=" + root_id +'\'' +
                ", parent_id=" + parent_id +'\'' +
                ", status=" + status +
                '}';
    }
}
