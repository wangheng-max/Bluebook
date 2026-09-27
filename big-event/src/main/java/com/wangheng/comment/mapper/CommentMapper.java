package com.wangheng.comment.mapper;

import com.wangheng.comment.pojo.Comment;
import com.wangheng.comment.pojo.CommentVO;
import java.util.List;
import org.apache.ibatis.annotations.*;




/**
 * 评论模块 Mapper（文章/商品通用评论 + 评论点赞）
 */
@Mapper
public interface CommentMapper {

    @Insert("insert into comment(target_type, target_id, user_id, content, parent_id, root_id, reply_user_id, score, images, order_id, create_time, update_time) " +
            "values(#{targetType}, #{targetId}, #{userId}, #{content}, #{parentId}, #{rootId}, #{replyUserId}, #{score}, #{images}, #{orderId}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Comment comment);

    @Select("select * from comment where id=#{id}")
    Comment findById(Integer id);

    /** 顶级评论总数 */
    @Select("select count(*) from comment where target_type=#{targetType} and target_id=#{targetId} and parent_id=0")
    Long countRoots(@Param("targetType") String targetType, @Param("targetId") Integer targetId);

    /**
     * 顶级评论分页：sort=likes 按点赞数排序（最热），否则按时间排序（最新）
     */
    @Select("<script>" +
            "select c.id, c.target_type as targetType, c.target_id as targetId, c.user_id as userId, c.content, " +
            "c.parent_id as parentId, c.root_id as rootId, c.reply_user_id as replyUserId, c.like_count as likeCount, " +
            "c.score as score, c.images as imagesJson, c.order_id as orderId, " +
            "c.create_time as createTime, u.nickname, u.user_pic as userPic " +
            "from comment c left join user u on c.user_id = u.id " +
            "where c.target_type=#{targetType} and c.target_id=#{targetId} and c.parent_id=0 " +
            "<choose>" +
            "<when test=\"sort == 'likes'\">order by c.like_count desc, c.id desc</when>" +
            "<otherwise>order by c.id desc</otherwise>" +
            "</choose>" +
            " limit #{offset}, #{pageSize}" +
            "</script>")
    List<CommentVO> pageRoots(@Param("targetType") String targetType, @Param("targetId") Integer targetId,
                              @Param("sort") String sort, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /** 批量查一批顶级评论下的全部回复（按时间正序），供列表页装配楼中楼预览 */
    @Select("<script>" +
            "select c.id, c.target_type as targetType, c.target_id as targetId, c.user_id as userId, c.content, " +
            "c.parent_id as parentId, c.root_id as rootId, c.reply_user_id as replyUserId, c.like_count as likeCount, " +
            "c.score as score, c.images as imagesJson, c.order_id as orderId, " +
            "c.create_time as createTime, u.nickname, u.user_pic as userPic, ru.nickname as replyNickname " +
            "from comment c " +
            "left join user u on c.user_id = u.id " +
            "left join user ru on c.reply_user_id = ru.id " +
            "where c.root_id in " +
            "<foreach collection='rootIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach>" +
            " order by c.id asc" +
            "</script>")
    List<CommentVO> selectRepliesByRootIds(@Param("rootIds") List<Integer> rootIds);

    /** 某顶级评论的回复分页（按时间正序，符合会话阅读顺序） */
    @Select("select c.id, c.target_type as targetType, c.target_id as targetId, c.user_id as userId, c.content, " +
            "c.parent_id as parentId, c.root_id as rootId, c.reply_user_id as replyUserId, c.like_count as likeCount, " +
            "c.score as score, c.images as imagesJson, c.order_id as orderId, " +
            "c.create_time as createTime, u.nickname, u.user_pic as userPic, ru.nickname as replyNickname " +
            "from comment c " +
            "left join user u on c.user_id = u.id " +
            "left join user ru on c.reply_user_id = ru.id " +
            "where c.root_id=#{rootId} order by c.id asc limit #{offset}, #{pageSize}")
    List<CommentVO> pageReplies(@Param("rootId") Integer rootId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    @Select("select count(*) from comment where root_id=#{rootId}")
    Long countReplies(Integer rootId);

    /** 点赞：幂等插入，返回影响行数（0=已点过） */
    @Insert("insert ignore into comment_like(comment_id, user_id, create_time) values(#{commentId}, #{userId}, now())")
    int insertLike(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    /** 取消点赞，返回影响行数（0=本来没点过） */
    @Delete("delete from comment_like where comment_id=#{commentId} and user_id=#{userId}")
    int deleteLike(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    @Update("update comment set like_count = like_count + 1 where id=#{commentId}")
    void incrLikeCount(Integer commentId);

    @Update("update comment set like_count = GREATEST(like_count - 1, 0) where id=#{commentId}")
    void decrLikeCount(Integer commentId);

    /** 当前用户在本批评论中的点赞状态 */
    @Select("<script>" +
            "select comment_id from comment_like where user_id=#{userId} and comment_id in " +
            "<foreach collection='commentIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</script>")
    List<Integer> selectLikedCommentIds(@Param("userId") Integer userId, @Param("commentIds") List<Integer> commentIds);

    /** 某顶级评论下全部回复的ID（删除根评论时级联清理点赞明细用） */
    @Select("select id from comment where root_id=#{rootId} and parent_id!=0")
    List<Integer> selectReplyIds(Integer rootId);

    @Delete("delete from comment where id=#{id}")
    void deleteById(Integer id);

    /** 删除顶级评论及其全部回复 */
    @Delete("delete from comment where id=#{rootId} or root_id=#{rootId}")
    void deleteRootCascade(Integer rootId);

    @Delete("<script>" +
            "delete from comment_like where comment_id in " +
            "<foreach collection='commentIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach>" +
            "</script>")
    void deleteLikesByCommentIds(@Param("commentIds") List<Integer> commentIds);

    /** 商品平均评分（只统计带分的顶级评论，无评分时 null） */
    @Select("select avg(score) from comment where target_type=#{targetType} and target_id=#{targetId} and score is not null")
    Double avgScore(@Param("targetType") String targetType, @Param("targetId") Integer targetId);

    /** 商品评分人数 */
    @Select("select count(*) from comment where target_type=#{targetType} and target_id=#{targetId} and score is not null")
    int countScored(@Param("targetType") String targetType, @Param("targetId") Integer targetId);
}
