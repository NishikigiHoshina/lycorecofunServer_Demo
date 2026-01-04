package com.lycorisfun.fun.Service;

import com.lycorisfun.fun.Entity.function;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FuncService {
    public List<function> listFuncStatus();
    public int insertFunc(function func);
    public int changeStatus(function func);
    public int funcStatus(String funcname);
    public List<String> getIndexIMG();
    public int addIndexIMG(String imglink);
    public List<function> getIndexImg();
    public function getFuncbyID(int id);
    public int deleteFuncbyID(function func);
}
