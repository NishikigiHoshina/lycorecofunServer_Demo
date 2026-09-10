package com.lycorisfun.fun.Service.impl;


import com.lycorisfun.fun.Entity.Post;
import com.lycorisfun.fun.Entity.PostBody;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.PostBodyMapper;
import com.lycorisfun.fun.Mapper.PostMapper;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.PostDocService;
import com.lycorisfun.fun.Service.PostService;
import com.lycorisfun.fun.util.PostDocText;
import com.lycorisfun.fun.util.TextValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {
    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    /** 帖子正文（结构化文档）另表存取 */
    @Autowired
    private PostBodyMapper postBodyMapper;

    /** 正文校验/规范化、摘要与首图抽取 */
    @Autowired
    private PostDocService postDocService;

    @Override
    public List<Post> findAll() {
        List<Post> postList = postMapper.findAll();
        if (postList == null || postList.size()==0) {
            // 抛业务异常，状态码 404
            throw new BusinessException(404, "查询失败：未找到任何帖子数据");
        }
        return postList;
    }

    @Override
    public List<Post> findlist(Integer findnum){
        if (findnum==null){
            findnum=50;
        }
        List<Post> postList = postMapper.findlist(findnum);
        if (postList == null || postList.size()==0) {
            throw new BusinessException(404,"查询失败：未找到任何帖子数据");
        }
        return postList;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "postDetail", key = "#id"),
            @CacheEvict(cacheNames = "postPage", allEntries = true)
    })
    @Override
    public int delById(Integer id, Integer callerId) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "删除失败：帖子ID无效（不能为null或负数或0）");
        }
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        // 属主/管理员校验：findById 缺行抛 404；同一 Impl 内自调用绕过缓存代理=一次直读
        Post existing = findById(id);
        checkCanModify(existing.getPost_userid(), callerId);
        int affectedRows = postMapper.delbyid(id);
        if (affectedRows == 0) {
            throw new BusinessException(404, "删除失败：ID为" + id + "的帖子不存在");
        }
        return affectedRows;
    }


    /**
     * 发帖。
     *
     * <p>正文与主表分两处落库：{@code posts.content} 存**纯文本摘要**（≤255 字，正好适配该列
     * 现有宽度，因此不必改列类型），结构化文档存 {@code post_bodies.doc}（MEDIUMTEXT）。
     * 两步必须原子，故加 {@code @Transactional}。</p>
     *
     * <p>落库的文档是**服务端产出的规范 JSON**，不是客户端原样提交的内容——这是本链路的信任边界。</p>
     */
    @CacheEvict(cacheNames = "postPage", allEntries = true)
    @Transactional
    @Override
    public void add(Post post) {
        if (post == null) {
            throw new BusinessException(400, "新增失败：帖子内容不能为null");
        }
        // 白名单校验 + 规范化（不合规抛 400）
        String canonicalDoc = postDocService.validateAndNormalize(post.getDoc());

        post.setCreated_at(LocalDateTime.now().toString());

        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            post.setTitle("无标题");
        }
        // 库为 utf8mb3：标题单独校验（正文已在校验器里过了一遍）
        TextValidator.requireStorable(post.getTitle(), "标题");
        post.setDoc(null);                                            // doc 不是 posts 的列
        post.setContent(postDocService.toExcerpt(canonicalDoc));      // 摘要
        String cover = postDocService.firstImageSrc(canonicalDoc);    // 正文首图 → 列表封面
        if (cover != null) {
            post.setImgurl(cover);
        }
        post.setStatus(1);
        post.setReply_count(0);
        post.setLike_count(0);
        post.setPost_username(userMapper.findById(post.getPost_userid()).getUserName());
        int affectedRows = postMapper.add(post);                      // useGeneratedKeys 回填 postid
        if (affectedRows != 1) {
            throw new BusinessException(500, "新增失败：插入数据未生效（影响行数：" + affectedRows + "）");
        }
        postBodyMapper.upsert(new PostBody(post.getPostid(), canonicalDoc, null));
        post.setDoc(canonicalDoc);                                    // 回填，供响应体直接渲染
        System.out.println("新增成功，影响："+affectedRows+"行");
    }

    @Override
    public void addmessage(Post post) {
        if (post == null) {
            throw new BusinessException(400, "新增失败：消息内容不能为null");
        }
        // 入库前字符校验（库 utf8mb3，拦截 emoji 等 4 字节字符）
        TextValidator.requireStorable(post.getContent(), "留言内容");
        TextValidator.requireStorable(post.getPost_username(), "昵称");
        TextValidator.requireStorable(post.getLink(), "个人主页链接");
        post.setCreated_at(LocalDateTime.now().toString());
        post.setStatus(3);
        int affectedRows = postMapper.add(post);
        if (affectedRows != 1) {
            throw new BusinessException(500, "新增失败：插入数据未生效（影响行数：" + affectedRows + "）");
        }
    }

    // 评论/回复写入：失效楼中楼缓存
    @CacheEvict(cacheNames = "replyList", allEntries = true)
    @Override
    public void addcontent(Post post) {
        if (post == null) {
            throw new BusinessException(400, "新增失败：参数缺失");
        }
        // 入库前字符校验（库 utf8mb3，拦截 emoji 等 4 字节字符）
        TextValidator.requireStorable(post.getContent(), "评论内容");
        post.setCreated_at(LocalDateTime.now().toString());
        post.setTitle(null);
        post.setStatus(1);
        post.setReply_count(0);
        post.setLike_count(0);
        // 回填作者昵称（与 add() 一致），userid 无效则留空，不阻断回复
        if (post.getPost_userid() > 0) {
            User u = userMapper.findById(post.getPost_userid());
            if (u != null) {
                post.setPost_username(u.getUserName());
            }
        }
        int affectedRows = postMapper.add(post);
        if (affectedRows != 1) {
            throw new BusinessException(500, "新增失败：插入数据未生效（影响行数：" + affectedRows + "）");
        }
        System.out.println("新增成功，影响："+affectedRows+"行");
    }


    @Cacheable(cacheNames = "postDetail", key = "#id")
    @Override
    public Post findById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "查询失败：帖子ID无效（不能为null或负数或0）");
        }
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(404, "查询失败：ID为" + id + "的帖子不存在");
        }
        // 挂上结构化正文（单独一张表，避免列表查询把大字段拖出来）。
        // 为 null 表示存量帖子：那时 content 里是短 HTML，由前端转换后渲染。
        post.setDoc(postBodyMapper.findDocByPostid(id));
        return post;
    }

    @Override
    public  List<Post> findByUser(String username){
        if (username==null || username.trim().length()==0){
            throw new BusinessException(400,"查询失败，没有该用户");
        }
        List<Integer>userid=new ArrayList<Integer>();
        List<User> userInfoList = userMapper.findByUsername(username);
        List<Post> postList = new ArrayList<Post>();
        for (User user : userInfoList) {
            userid.add(user.getUserId());
        }
        if (userid.isEmpty()){
            throw new BusinessException(400,"无结果");
        }
        for (Integer id : userid){
            postList.addAll(postMapper.findByUserid(id));
        }
        return postList;
    }

    @Cacheable(cacheNames = "replyList", key = "#id")
    @Override
    public List<Post> findContentPointaPost(Integer id){
        List<Post> replylist=postMapper.findByParentid(id);
        if (replylist==null || replylist.size()==0){
            System.out.println("404-查询无结果");
            return new ArrayList<>();   // 空结果返回空列表（可缓存），避免 null 写入缓存抛异常
        }
        return replylist;
    }

    @Override
    public List<Post> findReplyPointaPost(Integer id){

        return new ArrayList<>();
    }

    @Override
    public List<Post> findByTitle(String title) {
        if(title==null || title.trim().length()==0){
            throw new BusinessException(400,"params is null");
        }
        List<Post> list=postMapper.findByTitle(title);
        if (list==null || list.size()==0){
            throw new BusinessException(404,"not found any post");
        }
        System.out.println("find "+list.size()+" posts");
        return list;
    }


//    @Override
//    public void updatePostInfo(Post userInfo) {
//        if (userInfo == null) {
//            throw new BusinessException(400, "更新失败：帖子内容不能为null");
//        }
//        int affectedRows = postMapper.updatePostInfo(userInfo);
//        if (affectedRows != 1) {
//            throw new BusinessException(500, "更新失败：数据修改未生效（影响行数：" + affectedRows + "）");
//        }
//    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "postDetail", key = "#postInfo.postid"),
            @CacheEvict(cacheNames = "postPage", allEntries = true)
    })
    @Override
    public Post updatePostInfo(Post postInfo, Integer callerId){
        if (postInfo == null) {
            throw new BusinessException(400,"修改失败：参数为空");
        }
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (postInfo.getPostid() == null || postInfo.getPostid() <= 0) {
            throw new BusinessException(400,"修改失败：帖子ID无效");
        }
        // 属主/管理员校验：findById 缺行抛 404；同一 Impl 内自调用绕过缓存代理=一次直读
        Post existing = findById(postInfo.getPostid());
        boolean callerIsAdmin = isAdmin(callerId);
        if (existing.getPost_userid() != callerId && !callerIsAdmin) {
            throw new BusinessException(403, "无权操作：非作者或非管理员");
        }
        Post p=new Post();
        p.setPostid(existing.getPostid());//通过id匹配帖子对象
        // 作者列守卫：仅管理员且显式提供了新作者(>0)才允许改作者；否则一律保留现有作者，杜绝伪造或写成 0
        if (callerIsAdmin && postInfo.getPost_userid() != 0) {
            p.setPost_userid(postInfo.getPost_userid());
        } else {
            p.setPost_userid(existing.getPost_userid());
        }
        if(postInfo.getPost_username()!=null){
            p.setPost_username(postInfo.getPost_username());
        }
        if(postInfo.getContent()!=null){
            p.setContent(postInfo.getContent());
        }
        if(postInfo.getCreated_at()!=null){
            p.setCreated_at(postInfo.getCreated_at());
        }
        if(postInfo.getTitle()!=null){
            p.setTitle(postInfo.getTitle());
        }
        if(postInfo.getLink()!=null){
            p.setLink(postInfo.getLink());
        }
        if(postInfo.getImgurl()!=null){
            p.setImgurl(postInfo.getImgurl());
        }
        if(postInfo.getLike_count()!=0){
            p.setLike_count(postInfo.getLike_count());
        }
        if(postInfo.getReply_count()!=0){
            p.setReply_count(postInfo.getReply_count());
        }
        if(postInfo.getRoot_id()!=0){
            p.setRoot_id(postInfo.getRoot_id());
        }
        if(postInfo.getParent_id()!=0){
            p.setParent_id(postInfo.getParent_id());
        }

        int n=postMapper.updatePostInfo(p);
        System.out.println("修改成功，影响:"+n+"行");
        return p;
    }

    @Cacheable(cacheNames = "postPage")
    @Override
    public List<Post> findPageList(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        int offset = (page - 1) * size;
        List<Post> postList = postMapper.findPageList(offset, size);
        // 空页不算异常（翻到末页常见），返回空列表而非抛 404
        if (postList == null) {
            return new ArrayList<>();
        }
        // 列表视图只给纯文本预览，在进入缓存前就定型，避免缓存对象被 Controller 改写：
        // 新帖的 content 已经是摘要；存量帖的 content 是短 HTML，这里剥成纯文本。
        for (Post post : postList) {
            post.setContent(PostDocText.legacyExcerpt(post.getContent(), PostDocText.DEFAULT_EXCERPT_CHARS));
            post.setDoc(null);          // 列表不带正文，避免把大字段塞进缓存
        }
        return postList;
    }

    @Cacheable(cacheNames = "postPage")
    @Override
    public int countPostList() {
        return postMapper.countPostList();
    }

    /* ===== 写操作鉴权（属主 或 管理员） ===== */

    /* 是否管理员：用户表 status==3，DB 判定，不信任前端提交/文档型字段 */
    private boolean isAdmin(Integer callerId) {
        if (callerId == null) {
            return false;
        }
        User u = userMapper.findById(callerId);     // callerId 已判空，可安全拆箱
        return u != null && u.getStatus() == 3;
    }

    /* 校验当前登录用户能否操作属主为 owner 的内容：属主或管理员放行，否则 403 */
    private void checkCanModify(int owner, Integer callerId) {
        if (callerId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (owner != callerId && !isAdmin(callerId)) {
            throw new BusinessException(403, "无权操作：非作者或非管理员");
        }
    }
}
