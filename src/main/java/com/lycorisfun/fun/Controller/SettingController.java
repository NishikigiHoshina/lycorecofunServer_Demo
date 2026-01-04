package com.lycorisfun.fun.Controller;


import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Entity.function;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Service.FuncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效

public class SettingController {
    private static final String UPLOAD_ROOT = "D:\\Work\\javaWorkspace\\upload\\img\\";

    @Autowired
    FuncService funcService;

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

    @PostMapping("/updateStatus")
    @RequireToken
    public void updateStatus(@RequestParam String funcname, @RequestParam int status) {
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

    @PostMapping("/addIndexIMG")
    @RequireToken
    public Map<String, Object> addIndexIMG(MultipartFile file)throws IOException {
        // 图片原来的名字
        String oldFileName = file.getOriginalFilename();
        // 图片新名字
        String newFileName = UUID.randomUUID().toString() + oldFileName;
        String fileSavePath = UPLOAD_ROOT + newFileName;

        File f = new File(fileSavePath);
        // 写入本地磁盘
        file.transferTo(f);

        String imagePath = "http://localhost:12808/lycorisfunServer/upload/" + newFileName;
        funcService.addIndexIMG(imagePath);

        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "文件上传成功");
        map.put("dataobject", imagePath);
        return map;
    }

    @PostMapping("/deleteIndexIMG")
    @RequireToken
    public Map<String, Object> deleteIndexIMG(@RequestParam int id) {
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
