package com.wangheng.message.mapper;

import com.wangheng.message.pojo.Message;
import java.util.List;
import org.apache.ibatis.annotations.*;


@Mapper
public interface MessageMapper {

    // 插入消息（先落库保证不丢）
    @Insert("INSERT INTO message(from_user_id, to_user_id, msg_type, content, article_id, is_read, create_time) " +
            "VALUES(#{fromUserId}, #{toUserId}, #{msgType}, #{content}, #{articleId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Message message);

    // 与某好友的历史消息（分页，两人往来记录，按时间倒序）
    @Select("<script>" +
            "SELECT m.*, u.username AS fromUsername, u.user_pic AS fromAvatar, " +
            "a.title AS articleTitle, a.cover_img AS articleCover " +
            "FROM message m " +
            "JOIN user u ON m.from_user_id = u.id " +
            "LEFT JOIN article a ON m.article_id = a.id " +
            "WHERE ((m.from_user_id = #{userId} AND m.to_user_id = #{friendId}) " +
            "OR (m.from_user_id = #{friendId} AND m.to_user_id = #{userId})) " +
            "ORDER BY m.create_time DESC, m.id DESC" +
            "</script>")
    List<Message> findHistory(Integer userId, Integer friendId);

    // 我收到的未读消息（离线消息）
    @Select("SELECT m.*, u.username AS fromUsername, u.user_pic AS fromAvatar, " +
            "a.title AS articleTitle, a.cover_img AS articleCover " +
            "FROM message m " +
            "JOIN user u ON m.from_user_id = u.id " +
            "LEFT JOIN article a ON m.article_id = a.id " +
            "WHERE m.to_user_id = #{userId} AND m.is_read = 0 " +
            "ORDER BY m.create_time DESC")
    List<Message> findUnread(Integer userId);

    // 标记已读
    @Update("UPDATE message SET is_read = 1 WHERE id = #{id} AND to_user_id = #{userId}")
    void markRead(Integer id, Integer userId);

    // 与某好友的未读消息全部标记已读（打开聊天窗口时）
    @Update("UPDATE message SET is_read = 1 WHERE from_user_id = #{friendId} AND to_user_id = #{userId} AND is_read = 0")
    void markReadAll(Integer userId, Integer friendId);
}
