package com.wangheng.message.service.impl;

import com.wangheng.common.PageBean;
import com.wangheng.message.mapper.MessageMapper;
import com.wangheng.message.pojo.Message;
import com.wangheng.message.service.MessageService;











import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public Message save(Integer fromUserId, Integer toUserId, int msgType, String content, Integer articleId) {
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setMsgType(msgType);
        message.setContent(content);
        message.setArticleId(articleId);
        message.setIsRead(0);
        messageMapper.insert(message);
        return message;
    }

    @Override
    public PageBean<Message> history(Integer userId, Integer friendId, Integer pageNum, Integer pageSize) {
        PageBean<Message> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Message> messages = messageMapper.findHistory(userId, friendId);
        Page<Message> p = (Page<Message>) messages;

        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());

        // 打开聊天窗口时，把该好友发来的未读消息置为已读
        messageMapper.markReadAll(userId, friendId);
        return pb;
    }

    @Override
    public List<Message> offline(Integer userId) {
        List<Message> unread = messageMapper.findUnread(userId);
        for (Message m : unread) {
            messageMapper.markRead(m.getId(), userId);
        }
        return unread;
    }
}
