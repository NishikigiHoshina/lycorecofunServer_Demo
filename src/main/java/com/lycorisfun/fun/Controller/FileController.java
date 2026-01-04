package com.lycorisfun.fun.Controller;

import com.lycorisfun.fun.Annotation.RequireToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
@CrossOrigin(origins = "*")    // 3. 现在才会生效
public class FileController {
    @PostMapping("/uploadFile")
    @RequireToken
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {

        // 图片原来的名字
        String oldFileName = file.getOriginalFilename();
        // 图片新名字
        String newFileName = UUID.randomUUID().toString() + oldFileName;
        String fileSavePath = "D:/upload/" + newFileName;


        File f = new File(fileSavePath);
        // 写入本地磁盘
        file.transferTo(f);


        String imagePath = "http://localhost:8089/newsserver/upload/" + newFileName;

        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "文件上传成功");
        map.put("dataobject", imagePath);
        return map;
    }
}
