package com.lycorisfun.fun.Mapper;

import com.lycorisfun.fun.Entity.LogRecord;
import com.lycorisfun.fun.Entity.function;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LogMapper {
    public int insertLog(LogRecord log);
    public List<LogRecord> listLog();
}
