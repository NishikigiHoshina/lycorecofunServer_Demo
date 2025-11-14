package com.lycorisfun.fun.VO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PostListVO {
    private Long id;
    private String title;
    private String user;
    private LocalDateTime createTime;
}
