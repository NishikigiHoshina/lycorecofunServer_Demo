package com.lycorisfun.fun.Service.impl;

import com.lycorisfun.fun.Entity.function;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Mapper.FuncMapper;
import com.lycorisfun.fun.Service.FuncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FuncServiceImpl implements FuncService {
    @Autowired
    private FuncMapper funcMapper;


    @Override
    public List<function> listFuncStatus(){
        List<function> funcList = funcMapper.listFuncStatus();
        if(funcList != null){
            return funcList;
        }
        else
            throw new BusinessException(404,"找不到数据");
    }

    @Override
    public int insertFunc(function func){
        if(func!=null){
            int row=funcMapper.insertFunc(func);
            System.out.println("添加成功，影响:"+row+"行");
            return row;
        }
        else
            throw new BusinessException(500,"服务器内部错误");
    }

    @Override
    public int changeStatus(function func){
        if(func!=null){
            int row=funcMapper.changeStatus(func);
            System.out.println("修改成功，影响:"+row+"行");
            return row;
        }
        else
            throw new BusinessException(500,"服务器内部错误");
    }

    @Override
    public  int funcStatus(String funcname){
        if(funcname==null){
            throw new BusinessException(400,"请求格式错误");
        }
        return funcMapper.funcStatus(funcname).getFunction_status();
    }

    @Override
    public List<String> getIndexIMG(){
        List<function> list=funcMapper.findlink();
        List<String> linklist=new ArrayList<>();
        for(function func:list){
            linklist.add(func.getFunction_link());
        }
        if(linklist==null){
            throw new BusinessException(404,"查询不到数据");
        }
        return linklist;
    }

    @Override
    public int addIndexIMG(String img){
        if(img==null || img.isEmpty()){
            throw new BusinessException(400,"参数错误");
        }
        function func=new function(0,"IndexIMGlink",1,img);
        int row=funcMapper.insertFunc(func);
        System.out.println("插入成功，影响:"+row+"行");
        return row;
    }
}
