package com.wangheng.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待确认好友请求的展示对象，包含发起请求的用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestVO {
    private Integer relationId;   // 好友关系记录ID
    private Integer userId;       // 发起请求的用户ID
    private String username;      // 用户名
    private String nickname;      // 昵称
    private String avatar;        // 头像URL
    private LocalDateTime createTime; // 请求发送时间
}
