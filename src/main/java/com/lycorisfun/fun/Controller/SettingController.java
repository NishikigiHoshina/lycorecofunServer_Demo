package com.lycorisfun.fun.Controller;


import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.function;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.UserMapper;
import com.lycorisfun.fun.Service.FileStorageService;
import com.lycorisfun.fun.Service.FuncService;
import com.lycorisfun.fun.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）

public class SettingController {

    @Autowired
    FuncService funcService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileStorageService fileStorage;

    @PostMapping("/getfuncstatus")
    public boolean getfunstatus(@RequestParam String funcname) {
        if(funcname==null||funcname==""){
            throw new BusinessException(400,"请求参数缺失");
        }
        if(funcService.funcStatus(funcname)==0){
            return false;
        }else
            return true;
    }

    // 修改功能开关：系统级写操作，仅管理员
    @PostMapping("/updateStatus")
    @RequireToken
    public void updateStatus(@RequestParam String funcname, @RequestParam int status,
                             HttpServletRequest request) {
        AuthUtil.requireAdmin(request, userMapper);
        if(funcname==null||funcname==""){
            throw new BusinessException(400,"请求错误");
        }
        function func = new function(funcname,status);
        int row = funcService.changeStatus(func);
        if(row!=0){
            System.out.println("修改成功，影响:"+row+"行");
        }
    }

    @PostMapping("/getIndexIMG")
    public Map<String,Object> getIndexIMG() {
        Map<String,Object> map = new HashMap<>();
        List<function> list=funcService.getIndexImg();
        if(list!=null&&list.size()>0){
            map.put("code",200);
            map.put("msg", "success");
            map.put("data",list);
        }
        else {
            map.put("code",400);
            map.put("msg","failed");
        }
        return map;
    }

    // 上传首页宣传图：统一走 FileStorageService（路径配置化 + 安全校验），仅管理员
    @PostMapping("/addIndexIMG")
    @RequireToken
    public Map<String, Object> addIndexIMG(MultipartFile file, HttpServletRequest request) {
        AuthUtil.requireAdmin(request, userMapper);
        // store() 返回相对 URL（/upload/xxx.png），按请求 host/context 拼成绝对地址再入库/回前端
        String imagePath = toAbsolute(request, fileStorage.store(file, "index-img"));
        funcService.addIndexIMG(imagePath);

        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "文件上传成功");
        map.put("dataobject", imagePath);
        return map;
    }

    /** 相对 /upload/.. → 本机绝对 http(s)://host:port/context/upload/.. */
    private String toAbsolute(HttpServletRequest request, String relative) {
        String scheme = request.getScheme();
        String host = request.getHeader("Host");   // 含 host[:port]，跟随调用方实际访问地址
        StringBuilder sb = new StringBuilder(scheme).append("://");
        if (host != null && !host.isEmpty()) {
            sb.append(host);
        } else {
            sb.append(request.getServerName());
            int port = request.getServerPort();
            if (port != 80 && port != 443) {
                sb.append(':').append(port);
            }
        }
        if (request.getContextPath() != null) {
            sb.append(request.getContextPath());
        }
        return sb.append(relative).toString();
    }

    // 删除首页宣传图：仅管理员
    @PostMapping("/deleteIndexIMG")
    @RequireToken
    public Map<String, Object> deleteIndexIMG(@RequestParam int id, HttpServletRequest request) {
        AuthUtil.requireAdmin(request, userMapper);
        Map<String, Object> map = new HashMap<>();
        function func =funcService.getFuncbyID(id);
        if(func==null){
            throw new BusinessException(404,"not found");
        }
        func.setFunction_status(0);
        int row=funcService.deleteFuncbyID(func);
        if(row!=0){
            map.put("code", 200);
            map.put("msg","删除成功");
        }else {
            map.put("code", 500);
            map.put("msg","删除失败");
        }
        return map;
    }
}
