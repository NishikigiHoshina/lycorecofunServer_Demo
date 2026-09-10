package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.PostBody;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 帖子正文文档的读写。与 PostMapper 分表管理，避免主表查询把大字段拖出来。
 */
@Mapper
public interface PostBodyMapper {

    /** 新增或覆盖某帖的正文（postid 为主键，用 upsert 避免"先查后写"的竞态） */
    public int upsert(PostBody body);

    /** 取某帖的正文文档 JSON；不存在返回 null（存量帖子即为 null） */
    public String findDocByPostid(@Param("postid") Integer postid);

    /** 删除某帖的正文（随帖子物理删除时使用；软删不调用） */
    public int deleteByPostid(@Param("postid") Integer postid);
}
