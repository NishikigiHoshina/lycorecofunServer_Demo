package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.function;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FuncMapper {
    public List<function> listFuncStatus();
    public int insertFunc(function func);
    public int changeStatus(function func);
    public function funcStatus(String funcname);
    public function findfunc(String funcname);
    public List<function> findlink();
}
