package com.wangheng.article.service;

import com.wangheng.article.pojo.InteractStatusVO;







public interface InteractionService {

    /**
     * 点赞（SADD + 热点加分），返回最新点赞数
     */
    Long like(Integer articleId, Integer userId);

    /**
     * 取消点赞（SREM + 热点减分），返回最新点赞数
     */
    Long unlike(Integer articleId, Integer userId);

    /**
     * 收藏（SADD + 热点加分），返回最新收藏数
     */
    Long favorite(Integer articleId, Integer userId);

    /**
     * 取消收藏（SREM + 热点减分），返回最新收藏数
     */
    Long unfavorite(Integer articleId, Integer userId);

    /**
     * 我的互动状态（是否已赞/已藏 + 计数）
     */
    InteractStatusVO status(Integer articleId, Integer userId);

    /**
     * 转发文章给好友（校验好友关系 → 记录 + 计数 + 消息卡片推送）
     */
    void forward(Integer articleId, Integer fromUserId, Integer toUserId);
}
