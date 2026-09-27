package com.wangheng.article.service;

import com.wangheng.article.pojo.InteractStatusVO;
import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.common.PageBean;








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
     * 收藏到指定收藏夹（folderId=null 表示默认收藏夹）
     */
    Long favorite(Integer articleId, Integer userId, Integer folderId);

    /**
     * 我赞过的文章（个人主页"赞过"列表，PageHelper 分页）
     */
    PageBean<NoteSearchVO> likedList(Integer userId, Integer pageNum, Integer pageSize);

    /**
     * 某收藏夹内的文章（folderId=null 表示默认收藏夹，PageHelper 分页）
     */
    PageBean<NoteSearchVO> favoriteList(Integer userId, Integer folderId, Integer pageNum, Integer pageSize);

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

    /**
     * 我的收藏夹列表（首项恒为虚拟"默认收藏夹" id=null）
     */
    java.util.List<com.wangheng.article.pojo.FavoriteFolderVO> myFolders(Integer userId);

    /**
     * 创建收藏夹（重名报错）
     */
    void createFolder(Integer userId, String name);

    /**
     * 重命名收藏夹
     */
    void renameFolder(Integer userId, Integer folderId, String name);

    /**
     * 删除收藏夹（夹内收藏自动回退默认收藏夹）
     */
    void deleteFolder(Integer userId, Integer folderId);
}
