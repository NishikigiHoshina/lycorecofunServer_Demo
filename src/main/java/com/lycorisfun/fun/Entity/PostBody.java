package com.lycorisfun.fun.Entity;

/**
 * 帖子正文（结构化文档 JSON）。
 *
 * <p>与 {@link Post} 一比一关联（postid 即主键也是外键）。单独成表的原因：</p>
 * <ul>
 *   <li>{@code posts} 是一表多用（帖子/评论/留言共用），正文文档只属于"帖子"这一种，</li>
 *   <li>{@code posts.content} 是 varchar(255)，正好留作**纯文本摘要**，不必改列类型；</li>
 *   <li>正文可以很大（MEDIUMTEXT），放在主表会让列表查询把大字段一起拖出来。</li>
 * </ul>
 */
public class PostBody {

    private Integer postid;
    /** 规范化的正文文档 JSON（由 PostDocValidator 产出，schema v1） */
    private String doc;
    private String updated_at;

    public PostBody() {
    }

    public PostBody(Integer postid, String doc, String updated_at) {
        this.postid = postid;
        this.doc = doc;
        this.updated_at = updated_at;
    }

    public Integer getPostid() {
        return postid;
    }

    public void setPostid(Integer postid) {
        this.postid = postid;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }
}
