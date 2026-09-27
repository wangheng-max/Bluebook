package com.wangheng.friend.pojo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



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
