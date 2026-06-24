package com.wangheng.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友列表展示对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendVO {
    private Integer friendId;     // 好友的用户ID
    private String username;      // 用户名
    private String nickname;      // 昵称
    private String avatar;        // 头像URL
    private LocalDateTime createTime; // 成为好友的时间
    private Integer noteCount;    // 好友公开笔记数量
}
