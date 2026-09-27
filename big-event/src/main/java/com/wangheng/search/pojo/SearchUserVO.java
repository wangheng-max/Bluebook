package com.wangheng.search.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 用户搜索结果展示对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserVO {
    private Integer userId;       // 用户ID
    private String username;      // 用户名
    private String nickname;      // 昵称
    private String avatar;        // 头像URL
    private Boolean isFriend;     // 是否已经是好友
    private Boolean hasRequested; // 是否已有待处理的好友请求
}
