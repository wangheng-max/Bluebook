package com.wangheng.article.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;



/**
 * 点赞/收藏/转发明细表落库（定时任务对账写入）
 */
@Mapper
public interface InteractionMapper {

    // 点赞明细：幂等插入
    @Insert("INSERT IGNORE INTO article_like(article_id, user_id, create_time) VALUES(#{articleId}, #{userId}, NOW())")
    void insertLike(Integer articleId, Integer userId);

    // 取消赞：删除明细
    @Delete("DELETE FROM article_like WHERE article_id = #{articleId} AND user_id = #{userId}")
    void deleteLike(Integer articleId, Integer userId);

    // 点赞用户ID列表
    @Select("SELECT user_id FROM article_like WHERE article_id = #{articleId}")
    List<Integer> likeUserIds(Integer articleId);

    // 点赞对账：清空该文章全部点赞明细（Set 为空时）
    @Delete("DELETE FROM article_like WHERE article_id = #{articleId}")
    void deleteLikeAll(Integer articleId);

    // 点赞对账：删除不在 Redis Set 中的明细（取消赞的用户）
    @Delete("<script>" +
            "DELETE FROM article_like WHERE article_id = #{articleId} AND user_id NOT IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>" +
            "</script>")
    void deleteLikeNotIn(Integer articleId, List<Integer> userIds);

    // 收藏明细：幂等插入
    @Insert("INSERT IGNORE INTO article_favorite(article_id, user_id, folder_id, create_time) " +
            "VALUES(#{articleId}, #{userId}, #{folderId}, NOW())")
    void insertFavorite(Integer articleId, Integer userId, Integer folderId);

    // 取消收藏：删除明细
    @Delete("DELETE FROM article_favorite WHERE article_id = #{articleId} AND user_id = #{userId}")
    void deleteFavorite(Integer articleId, Integer userId);

    // 收藏用户ID列表
    @Select("SELECT user_id FROM article_favorite WHERE article_id = #{articleId}")
    List<Integer> favoriteUserIds(Integer articleId);

    // 收藏对账：清空该文章全部收藏明细
    @Delete("DELETE FROM article_favorite WHERE article_id = #{articleId}")
    void deleteFavAll(Integer articleId);

    // 收藏对账：删除不在 Redis Set 中的明细
    @Delete("<script>" +
            "DELETE FROM article_favorite WHERE article_id = #{articleId} AND user_id NOT IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>" +
            "</script>")
    void deleteFavNotIn(Integer articleId, List<Integer> userIds);

    // 转发明细：幂等插入
    @Insert("INSERT IGNORE INTO article_forward(article_id, from_user_id, to_user_id, create_time) VALUES(#{articleId}, #{fromUserId}, #{toUserId}, NOW())")
    void insertForward(Integer articleId, Integer fromUserId, Integer toUserId);

    // 同步文章计数（定时任务：以 Redis 计数为准回写）
    @Update("UPDATE article SET like_count = #{likeCount}, favorite_count = #{favoriteCount} WHERE id = #{articleId}")
    void syncCounts(Integer articleId, int likeCount, int favoriteCount);

    // 转发计数累加（转发为低频写，直接落库）
    @Update("UPDATE article SET forward_count = forward_count + 1 WHERE id = #{articleId}")
    void incrForwardCount(Integer articleId);

    // 浏览计数累加（定时任务：Redis INCR 增量批量落库）
    @Update("UPDATE article SET view_count = view_count + #{delta} WHERE id = #{articleId}")
    void incrViewCount(Integer articleId, int delta);

    /** 我赞过的文章（个人主页"赞过"列表，新的在前；PageHelper 分页） */
    @Select("select a.* from article_like l join article a on a.id = l.article_id " +
            "where l.user_id = #{userId} order by l.create_time desc, l.id desc")
    List<com.wangheng.article.pojo.Article> listLikedArticles(Integer userId);

    /** 某收藏夹内的文章（folderId=null 表示默认收藏夹 folder_id IS NULL） */
    @Select("<script>" +
            "select a.* from article_favorite f join article a on a.id = f.article_id " +
            "where f.user_id = #{userId} " +
            "<choose>" +
            "<when test='folderId == null'>and f.folder_id is null</when>" +
            "<otherwise>and f.folder_id = #{folderId}</otherwise>" +
            "</choose>" +
            " order by f.create_time desc, f.id desc" +
            "</script>")
    List<com.wangheng.article.pojo.Article> listFavoriteArticles(@Param("userId") Integer userId,
                                                                 @Param("folderId") Integer folderId);

    /** 自定义收藏夹列表（带夹内文章数，按创建时间正序） */
    @Select("select ff.id, ff.name, ff.create_time as createTime, " +
            "count(f.id) as articleCount " +
            "from favorite_folder ff " +
            "left join article_favorite f on f.folder_id = ff.id and f.user_id = #{userId} " +
            "where ff.user_id = #{userId} " +
            "group by ff.id, ff.name, ff.create_time order by ff.create_time asc, ff.id asc")
    List<com.wangheng.article.pojo.FavoriteFolderVO> listFolders(Integer userId);

    /** 默认收藏夹（folder_id 为 NULL）的文章数 */
    @Select("select count(*) from article_favorite where user_id = #{userId} and folder_id is null")
    int countDefaultFolderArticles(Integer userId);
}
