package com.wangheng.message.service;

import com.wangheng.common.PageBean;
import com.wangheng.message.pojo.Message;
import java.util.List;



public interface MessageService {

    /**
     * 保存消息并返回（含发送者信息），用于落库后推送
     */
    Message save(Integer fromUserId, Integer toUserId, int msgType, String content, Integer articleId);

    /**
     * 与某好友的历史消息（分页，时间倒序）
     */
    PageBean<Message> history(Integer userId, Integer friendId, Integer pageNum, Integer pageSize);

    /**
     * 我的未读/离线消息（拉取后标记已读）
     */
    List<Message> offline(Integer userId);
}
