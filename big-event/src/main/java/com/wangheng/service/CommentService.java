package com.wangheng.service;

import com.wangheng.pojo.CommentSaveDTO;
import com.wangheng.pojo.CommentVO;
import com.wangheng.pojo.PageBean;

/**
 * 评论服务（文章/商品通用）：发布、回复、点赞、按时间/点赞排序的分页查询
 */
public interface CommentService {

    /** 发布评论/回复 */
    void add(CommentSaveDTO dto, Integer userId);

    /** 顶级评论分页（sort=time 最新在前 / likes 按点赞数），每条带楼中楼预览与当前用户点赞状态 */
    PageBean<CommentVO> list(String targetType, Integer targetId, String sort, Integer pageNum, Integer pageSize);

    /** 某顶级评论下的回复分页（按时间正序） */
    PageBean<CommentVO> replies(Integer rootId, Integer pageNum, Integer pageSize);

    /** 点赞评论，返回最新点赞数 */
    Long like(Integer commentId, Integer userId);

    /** 取消点赞，返回最新点赞数 */
    Long unlike(Integer commentId, Integer userId);

    /** 删除自己的评论（顶级评论连带全部回复） */
    void delete(Integer commentId, Integer userId);
}
