package com.wangheng.friend.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRelation {
    private Integer id;
    private Integer userId;
    private Integer friendId;
    private Integer status; // 0-待确认, 1-已接受, 2-已拒绝
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
