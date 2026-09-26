package com.wangheng.friend.mapper;

import com.wangheng.friend.pojo.FriendRelation;
import com.wangheng.friend.pojo.FriendRequestVO;
import com.wangheng.friend.pojo.FriendVO;









import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendMapper {

    // 检查是否已存在好友关系记录（任意状态）
    @Select("SELECT * FROM friend_relation WHERE " +
            "((user_id = #{userId} AND friend_id = #{friendId}) " +
            "OR (user_id = #{friendId} AND friend_id = #{userId})) " +
            "LIMIT 1")
    FriendRelation findRelation(Integer userId, Integer friendId);

    // 发送好友请求
    @Insert("INSERT INTO friend_relation(user_id, friend_id, status, create_time, update_time) " +
            "VALUES(#{userId}, #{friendId}, 0, NOW(), NOW())")
    void insertRequest(Integer userId, Integer friendId);

    // 获取发送给当前用户的待确认请求列表（联表查询发起人信息）
    @Select("SELECT fr.id AS relationId, u.id AS userId, u.username, u.nickname, " +
            "u.user_pic AS avatar, fr.create_time AS createTime " +
            "FROM friend_relation fr " +
            "JOIN user u ON fr.user_id = u.id " +
            "WHERE fr.friend_id = #{currentUserId} AND fr.status = 0 " +
            "ORDER BY fr.create_time DESC")
    List<FriendRequestVO> findPendingRequests(Integer currentUserId);

    // 根据ID查找好友关系记录
    @Select("SELECT * FROM friend_relation WHERE id = #{relationId}")
    FriendRelation findById(Integer relationId);

    // 确认好友请求
    @Update("UPDATE friend_relation SET status = 1, update_time = NOW() WHERE id = #{relationId}")
    void confirmRequest(Integer relationId);

    // 拒绝好友请求
    @Update("UPDATE friend_relation SET status = 2, update_time = NOW() WHERE id = #{relationId}")
    void rejectRequest(Integer relationId);

    // 获取已确认的好友列表（双向关系，含公开笔记数量）
    @Select("<script>" +
            "SELECT u.id AS friendId, u.username, u.nickname, u.user_pic AS avatar, " +
            "fr.create_time AS createTime, " +
            "(SELECT COUNT(*) FROM article a WHERE a.create_user = u.id AND a.state = '已发布') AS noteCount " +
            "FROM user u " +
            "JOIN friend_relation fr ON (fr.user_id = u.id AND fr.friend_id = #{currentUserId}) " +
            "OR (fr.friend_id = u.id AND fr.user_id = #{currentUserId}) " +
            "WHERE fr.status = 1 AND u.id != #{currentUserId} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "GROUP BY u.id, u.username, u.nickname, u.user_pic, fr.create_time " +
            "ORDER BY fr.create_time DESC" +
            "</script>")
    List<FriendVO> findFriends(Integer currentUserId, String keyword);

    // 删除好友（删除双方关系记录）
    @Delete("DELETE FROM friend_relation WHERE status = 1 AND " +
            "((user_id = #{userId} AND friend_id = #{friendId}) " +
            "OR (user_id = #{friendId} AND friend_id = #{userId}))")
    int deleteFriendRelation(Integer userId, Integer friendId);

    // 检查是否为好友（status=1）
    @Select("SELECT COUNT(*) FROM friend_relation WHERE status = 1 AND " +
            "((user_id = #{userId} AND friend_id = #{friendId}) " +
            "OR (user_id = #{friendId} AND friend_id = #{userId}))")
    int isFriend(Integer userId, Integer friendId);

    // 检查是否有待处理的好友请求
    @Select("SELECT COUNT(*) FROM friend_relation WHERE status = 0 AND " +
            "user_id = #{userId} AND friend_id = #{friendId}")
    int hasPendingRequest(Integer userId, Integer friendId);
}
