package com.wangheng.message.controller;

import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.message.pojo.Message;
import com.wangheng.message.service.MessageService;













import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 与某好友的历史消息
     */
    @GetMapping("/history")
    public Result<PageBean<Message>> history(
            @RequestParam Integer friendId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(messageService.history(userId, friendId, pageNum, pageSize));
    }

    /**
     * 上线后拉取未读/离线消息
     */
    @GetMapping("/offline")
    public Result<List<Message>> offline() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(messageService.offline(userId));
    }
}
