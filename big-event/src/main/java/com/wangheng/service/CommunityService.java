package com.wangheng.service;

import com.wangheng.pojo.NoteSearchVO;
import com.wangheng.pojo.PageBean;

import java.util.List;

public interface CommunityService {

    /**
     * 热点文章分页（Redis ZSET 取 Top N，冷启动时按浏览量查库兜底）
     */
    PageBean<NoteSearchVO> hotArticles(Integer pageNum, Integer pageSize);

    /**
     * 按分类查询已发布文章（PageHelper 分页 + 联合索引）
     */
    PageBean<NoteSearchVO> articlesByCategory(Integer categoryId, Integer pageNum, Integer pageSize);

    /**
     * 文章详情（含计数）
     */
    NoteSearchVO articleDetail(Integer articleId);
}
