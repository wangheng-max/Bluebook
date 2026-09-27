package com.wangheng.message.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 聊天消息（msg_type: 0-文本 1-文章转发）
 */
@Data
public class Message {
    private Integer id;
    private Integer fromUserId;   // 发送者
    private Integer toUserId;     // 接收者
    private Integer msgType;      // 0-文本 1-文章转发
    private String content;       // 文本内容
    private Integer articleId;    // 转发文章ID（msg_type=1时）
    private Integer isRead;       // 0-未读 1-已读

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 冗余展示字段（联表查询填充）
    private String fromUsername;
    private String fromAvatar;
    // 转发卡片文章信息（msg_type=1时）
    private String articleTitle;
    private String articleCover;
}
