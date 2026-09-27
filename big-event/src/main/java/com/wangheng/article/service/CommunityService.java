package com.wangheng.article.service;

import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.common.PageBean;
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

    /**
     * 某作者的已发布文章（博主主页作品列表，PageHelper 分页）
     */
    PageBean<NoteSearchVO> articlesByUser(Integer userId, Integer pageNum, Integer pageSize);

    /**
     * 热门标签 Top N（发布表单联想 + 社区标签入口）
     */
    java.util.List<String> hotTags(int limit);
}
